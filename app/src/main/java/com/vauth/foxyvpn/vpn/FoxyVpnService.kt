package com.vauth.foxyvpn.vpn

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.VpnService
import android.net.wifi.WifiManager
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.vauth.foxyvpn.FoxyVpnApp
import com.vauth.foxyvpn.MainActivity
import com.vauth.foxyvpn.data.AppLogger
import com.vauth.foxyvpn.data.ControlPlaneHttp
import com.vauth.foxyvpn.data.FxaAuthRepository
import com.vauth.foxyvpn.data.GUARDIAN_ENDPOINT_DEFAULT
import com.vauth.foxyvpn.data.GuardianClient
import com.vauth.foxyvpn.data.ProxyPass
import com.vauth.foxyvpn.data.ProxyStateStore
import com.vauth.foxyvpn.data.QuotaExceededError
import com.vauth.foxyvpn.data.RECOMMENDED_COUNTRY_CODE
import com.vauth.foxyvpn.data.ServerListClient
import com.vauth.foxyvpn.data.SettingsStore
import com.vauth.foxyvpn.data.TokenInvalidError
import com.vauth.foxyvpn.data.TokenStore
import com.vauth.foxyvpn.data.formatBytesPerSecond
import com.vauth.foxyvpn.data.model.AccountLiveStats
import com.vauth.foxyvpn.data.model.ConnectionState
import com.vauth.foxyvpn.data.model.ProxyCandidate
import com.vauth.foxyvpn.data.model.VpnAccount
import com.vauth.foxyvpn.vpn.socks.LocalHttpProxyServer
import com.vauth.foxyvpn.vpn.socks.LocalSocks5Server
import com.vauth.foxyvpn.vpn.tun.HevSocks5Tunnel
import com.vauth.foxyvpn.vpn.tun.HevSocks5TunnelConfig
import com.vauth.foxyvpn.vpn.upstream.EdgeAddressResolver
import com.vauth.foxyvpn.vpn.upstream.MultiUpstreamSessionPool
import com.vauth.foxyvpn.vpn.upstream.UpstreamProxyConfig
import com.vauth.foxyvpn.vpn.upstream.UpstreamSession
import com.vauth.foxyvpn.widget.QuickConnectWidgetProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

private const val TAG = "FoxyVpnService"
private const val CONNECT_TIMEOUT_MS = 20_000L
private const val SERVER_LIST_FETCH_TIMEOUT_MS = 15_000L

private const val UPSTREAM_WATCHDOG_INTERVAL_MS = 2_000L

private const val RECONNECT_FAILURES_BEFORE_WARNING = 5
private const val RECONNECT_BACKOFF_BASE_MS = 3_000L
private const val RECONNECT_BACKOFF_CAP_MS = 60_000L

private const val RECONNECT_FAILURES_BEFORE_EDGE_ROTATION = 2

private const val MAX_ALTERNATE_EDGES = 3

private const val UNHEALTHY_REDIAL_COOLDOWN_MS = 30_000L

private const val PROXY_PASS_RENEWAL_FRACTION = 0.5
private const val PROXY_PASS_RENEWAL_SAFETY_MARGIN_MS = 30_000L

private const val PROXY_PASS_RENEWAL_FLOOR_MS = 15_000L

private const val PROXY_PASS_RENEWAL_CEILING_MS = 30 * 60_000L

private const val PROXY_PASS_RENEWAL_FALLBACK_MS = 4 * 60_000L

private const val PROXY_PASS_RENEWAL_RETRY_MS = 30_000L

private const val POST_TUN_SETTLE_MS = 250L

private const val INITIAL_DIAL_SETTLE_MS = 600L

private const val INITIAL_DIAL_MAX_ATTEMPTS = MAX_ALTERNATE_EDGES + 1
private const val INITIAL_DIAL_BACKOFF_BASE_MS = 2_000L
private const val INITIAL_DIAL_BACKOFF_CAP_MS = 8_000L

private const val SPEED_UPDATE_INTERVAL_MS = 2_000L
/** v1.3 lag fix: EncryptedSharedPreferences writes are slow — throttle usage persistence. */
private const val USAGE_PERSIST_INTERVAL_MS = 30_000L

private const val TUN_ADDRESS_IPV6 = "fd00:1:fd00:1:fd00:1:fd00:1"
private const val TUN_PREFIX_LENGTH_IPV6 = 128

private const val WAKE_LOCK_TAG = "com.vauth.foxyvpn:vpn"
private const val WIFI_LOCK_TAG = "com.vauth.foxyvpn:vpn-wifi"

private fun fullJitterBackoffMs(attempt: Int, baseMs: Long, capMs: Long): Long {
    val exponential = baseMs * (1L shl attempt.coerceIn(0, 10))
    val upperBound = exponential.coerceAtMost(capMs)
    return (upperBound.toDouble() * Math.random()).toLong().coerceAtLeast(baseMs / 4)
}

private fun proxyPassRenewalDelayMs(expiresAtEpochSeconds: Long?): Long {
    if (expiresAtEpochSeconds == null) return PROXY_PASS_RENEWAL_FALLBACK_MS
    val remainingMs = expiresAtEpochSeconds * 1_000L - System.currentTimeMillis()
    if (remainingMs <= 0L) return PROXY_PASS_RENEWAL_FLOOR_MS
    val atHalfLife = (remainingMs * PROXY_PASS_RENEWAL_FRACTION).toLong()
    val beforeExpiry = remainingMs - PROXY_PASS_RENEWAL_SAFETY_MARGIN_MS
    return minOf(atHalfLife, beforeExpiry)
        .coerceAtMost(PROXY_PASS_RENEWAL_CEILING_MS)
        .coerceAtLeast(PROXY_PASS_RENEWAL_FLOOR_MS)
}

class FoxyVpnService : VpnService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val opMutex = Mutex()

    private var tunFd: ParcelFileDescriptor? = null
    private var socksServer: LocalSocks5Server? = null
    private var httpProxyServer: LocalHttpProxyServer? = null
    private var upstreamSession: UpstreamSession? = null
    private var connectJob: Job? = null
    private var watchdogJob: Job? = null
    private var speedJob: Job? = null

    private var tokenRenewalJob: Job? = null

    private var poolStatsJob: Job? = null
    private var lastPersistedBytes: Map<String, Long> = emptyMap()
    private var lastPersistAt: Long = 0L

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    @Volatile private var tunnelInterfaceActive = false

    @Volatile private var boundNetwork: Network? = null

    @Volatile private var reportedUnderlying: Network? = null

    @Volatile private var lastUnhealthyRedialAt = 0L

    @Volatile private var statusLabel: String = "Connecting\u2026"

    @Volatile private var foregroundActive = false

    @Volatile private var lastNotificationText: String? = null

    private class SessionResources(
        val tunFd: ParcelFileDescriptor?,
        val socksServer: LocalSocks5Server?,
        val httpProxyServer: LocalHttpProxyServer?,
        val upstreamSession: UpstreamSession?,
    ) {
        val isEmpty: Boolean get() = tunFd == null && socksServer == null && httpProxyServer == null && upstreamSession == null
    }

    @Volatile
    private var connectionGeneration = 0

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            reportUnderlyingNetwork(network)
        }

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            reportUnderlyingNetwork(network, capabilities)
        }

        override fun onLost(network: Network) {
            if (boundNetwork != network) return
            boundNetwork = null
            reportedUnderlying = null

            if (tunnelInterfaceActive) {
                runCatching { setUnderlyingNetworks(null) }
            }
        }
    }

    private fun reportUnderlyingNetwork(
        network: Network,
        capabilities: NetworkCapabilities? = null,
    ) {
        val caps = capabilities
            ?: getSystemService(ConnectivityManager::class.java)?.getNetworkCapabilities(network)

        if (caps == null) return

        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) return

        if (tunnelInterfaceActive && reportedUnderlying != network) {
            reportedUnderlying = network
            runCatching { setUnderlyingNetworks(arrayOf(network)) }
                .onFailure {

                    reportedUnderlying = null
                    AppLogger.w(TAG, "failed to report the underlying network to the platform", it)
                }
        }

        val previous = boundNetwork
        if (previous == network) return

        if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) && previous != null) return
        boundNetwork = network
        runCatching { ControlPlaneHttp.client.connectionPool.evictAll() }
        if (previous == null) return

        if (_state.value != ConnectionState.CONNECTED) return
        val session = upstreamSession ?: return

        AppLogger.i(
            TAG,
            "underlying network changed; closing the upstream session bound to the old one so it is redialed now " +
                "(its socket would otherwise stay ESTABLISHED and look healthy for minutes)",
        )
        runCatching { session.close() }
    }

    private fun onUpstreamSessionUnhealthy() {
        if (_state.value != ConnectionState.CONNECTED) return
        val session = upstreamSession ?: return
        val now = System.currentTimeMillis()
        val sinceLast = now - lastUnhealthyRedialAt
        if (lastUnhealthyRedialAt != 0L && sinceLast < UNHEALTHY_REDIAL_COOLDOWN_MS) {
            AppLogger.d(
                TAG,
                "ignoring an unhealthy-session verdict ${sinceLast}ms after the last rebuild " +
                    "(cooldown ${UNHEALTHY_REDIAL_COOLDOWN_MS}ms)",
            )
            return
        }
        lastUnhealthyRedialAt = now
        AppLogger.w(
            TAG,
            "the local proxy reports the upstream session is failing as a whole rather than for one destination; " +
                "closing it so the watchdog redials",
        )
        runCatching { session.close() }
    }

    override fun onCreate() {
        super.onCreate()
        runCatching {

            val underlyingNetworks = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
                .build()
            getSystemService(ConnectivityManager::class.java)
                ?.registerNetworkCallback(underlyingNetworks, networkCallback)
        }.onFailure { AppLogger.w(TAG, "could not register a network callback; handovers will be slower to detect", it) }

        scope.launch {
            _state.collect { state ->
                QuickConnectWidgetProvider.updateAll(this@FoxyVpnService, state)
            }
        }
    }

    override fun onDestroy() {
        ControlPlaneHttp.socketProtector = null
        runCatching {
            getSystemService(ConnectivityManager::class.java)?.unregisterNetworkCallback(networkCallback)
        }
        connectJob?.cancel()
        connectJob = null
        watchdogJob?.cancel()
        watchdogJob = null
        connectionGeneration++
        releaseResources(detachResources(), stopNativeTunnel = true)
        releaseWakeLocks()
        foregroundActive = false
        lastNotificationText = null
        boundNetwork = null
        reportedUnderlying = null
        scope.cancel()
        _state.value = ConnectionState.DISCONNECTED
        QuickConnectWidgetProvider.updateAll(this, ConnectionState.DISCONNECTED)
        super.onDestroy()
    }

    private fun acquireWakeLocks() {
        if (wifiLock == null) {
            runCatching {
                val wifi = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                    ?: error("WifiManager unavailable")
                @Suppress("DEPRECATION")
                wifiLock = wifi.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, WIFI_LOCK_TAG).apply {
                    setReferenceCounted(false)
                    acquire()
                }
            }.onFailure { AppLogger.d(TAG, "Wi-Fi high-performance lock unavailable: ${it.message}") }
        }
    }

    private fun releaseWakeLocks() {
        runCatching { wifiLock?.takeIf { it.isHeld }?.release() }
            .onFailure { AppLogger.d(TAG, "error releasing the Wi-Fi lock: ${it.message}") }
        wifiLock = null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val action = intent?.action ?: ACTION_CONNECT
        if (action == ACTION_CONNECT || action == VpnService.SERVICE_INTERFACE) {

            if (_state.value != ConnectionState.DISCONNECTED) {
                AppLogger.d(TAG, "ignoring duplicate connect request while ${_state.value}")

                enterForeground(statusLabel)
                return START_STICKY
            }
            if (intent == null) {
                AppLogger.i(TAG, "the service was restarted by the system; bringing the tunnel back up")
            }
            _state.value = ConnectionState.CONNECTING
            _lastError.value = null
            statusLabel = "Connecting\u2026"
            enterForeground(statusLabel)

            acquireWakeLocks()
            connectJob = scope.launch { opMutex.withLock { connect() } }

            return START_STICKY
        } else if (action == ACTION_DISCONNECT) {
            requestDisconnect("requested by the user")
        }
        return START_NOT_STICKY
    }

    private fun requestDisconnect(reason: String) {
        val idle = _state.value == ConnectionState.DISCONNECTED &&
            connectJob == null &&
            watchdogJob == null
        if (idle) {
            releaseWakeLocks()
            stopSelf()
            return
        }
        AppLogger.i(TAG, "disconnect: $reason")

        val endedGeneration = ++connectionGeneration
        _state.value = ConnectionState.DISCONNECTED
        connectJob?.cancel()
        connectJob = null
        watchdogJob?.cancel()
        watchdogJob = null
        exitForeground()

        releaseWakeLocks()

        val doomed = detachResources()

        scope.launch {
            opMutex.withLock {

                val stillOurs = connectionGeneration == endedGeneration
                releaseResources(doomed, stopNativeTunnel = stillOurs)

                if (stillOurs && _state.value == ConnectionState.DISCONNECTED) {
                    stopSelf()
                }
            }
        }
    }

    private fun ensureGenerationCurrent(myGeneration: Int) {
        if (myGeneration != connectionGeneration) {
            throw CancellationException("connect was superseded by a newer request")
        }
    }

    private fun firstUsableNetwork(): Network? {
        val manager = getSystemService(ConnectivityManager::class.java) ?: return null
        @Suppress("DEPRECATION")
        val networks = runCatching { manager.allNetworks }.getOrDefault(emptyArray())
        var unvalidated: Network? = null
        for (network in networks) {
            val capabilities = manager.getNetworkCapabilities(network) ?: continue

            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) continue
            if (!capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) continue
            if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) return network
            if (unvalidated == null) unvalidated = network
        }
        return unvalidated
    }

    private fun hasUsableNetwork(): Boolean {

        getSystemService(ConnectivityManager::class.java) ?: return true
        return firstUsableNetwork() != null
    }

    private fun isFatalUpstreamError(error: Throwable): Boolean =
        error is TokenInvalidError || error is QuotaExceededError

    /**
     * Mints a Guardian proxy pass for one account (multi-account patch).
     * The account's FxA access token is renewed first when expired; the
     * renewed auth is persisted back to the account list.
     */
    private suspend fun mintProxyPassFor(tokenStore: TokenStore, account: VpnAccount): ProxyPass {
        var auth = account.auth
        val nowSeconds = System.currentTimeMillis() / 1000
        if (auth.expiresAtEpochSeconds - nowSeconds <= 60L) {
            val renewed = runCatching {
                FxaAuthRepository(tokenStore).ensureFreshAccessTokenForAccount(account, force = true)
            }.getOrNull()
            if (renewed != null) {
                auth = renewed
                tokenStore.updateAccount(account.copy(auth = renewed, lastError = null))
                AppLogger.i(TAG, "the access token for ${account.email} had expired and was renewed")
            } else {
                AppLogger.w(
                    TAG,
                    "the access token for ${account.email} is past its expiry and could not be renewed " +
                        "automatically; trying with the stored one",
                )
            }
        }
        val guardian = GuardianClient()

        return try {
            guardian.fetchProxyPass(GUARDIAN_ENDPOINT_DEFAULT, auth.accessToken)
        } catch (invalid: TokenInvalidError) {
            AppLogger.w(
                TAG,
                "proxy pass rejected for ${account.email}, activating Guardian entitlement and retrying",
                invalid,
            )
            guardian.activateGuardian(GUARDIAN_ENDPOINT_DEFAULT, auth.accessToken)
            guardian.fetchProxyPass(GUARDIAN_ENDPOINT_DEFAULT, auth.accessToken)
        }
    }

    /**
     * Multi-account proxy-pass renewal: on each cycle every enabled account
     * gets a fresh pass minted and pushed into its pool entry in place, so the
     * tunnel is never rebuilt. The cycle is scheduled at half the remaining
     * life of the shortest-lived pass.
     */
    private fun startMultiAccountProxyPassRenewal(myGeneration: Int) {
        tokenRenewalJob?.cancel()
        tokenRenewalJob = scope.launch {
            AppLogger.i(TAG, "proxy pass renewal armed for the account pool")
            while (isActive) {
                val pool = upstreamSession as? MultiUpstreamSessionPool
                val delayMs = pool?.minPassExpiry()?.let { proxyPassRenewalDelayMs(it) }
                    ?: PROXY_PASS_RENEWAL_FALLBACK_MS
                delay(delayMs)
                if (myGeneration != connectionGeneration) return@launch

                val live = upstreamSession as? MultiUpstreamSessionPool
                if (live == null || !live.isConnected) {
                    AppLogger.d(
                        TAG,
                        "skipping proxy pass renewal: no live pool (the watchdog's redial builds its own)",
                    )
                    continue
                }

                val store = TokenStore(applicationContext)
                var renewed = 0
                for (account in store.getEnabledAccounts()) {
                    if (myGeneration != connectionGeneration) return@launch
                    val pass = runCatching { mintProxyPassFor(store, account) }.getOrNull()
                    if (pass == null) {
                        AppLogger.w(TAG, "proxy pass renewal failed for ${account.email}; retrying next cycle")
                        continue
                    }
                    live.updateAccountToken(account.id, pass.token)
                    live.setPassExpiry(account.id, pass.expiresAtEpochSeconds)
                    renewed++
                }
                AppLogger.i(TAG, "renewed proxy passes for $renewed account(s) in place; the tunnel was not rebuilt")
            }
        }
    }

    private suspend fun connect() {

        val myGeneration = ++connectionGeneration

        lastUnhealthyRedialAt = 0L

        boundNetwork = null

        EdgeAddressResolver.invalidate()
        runCatching {
            AppLogger.i(TAG, "connect: starting")
            val tokenStore = TokenStore(applicationContext)
            // Multi-account patch: connect with every enabled account; the
            // legacy single session migrates into the list on first run.
            val accounts = tokenStore.getEnabledAccounts().toMutableList()
            if (accounts.isEmpty()) {
                tokenStore.migrateLegacySessionIfPresent()?.let { accounts.add(it) }
            }
            if (accounts.isEmpty()) error("Not signed in")
            val proxyStateStore = ProxyStateStore(applicationContext)
            val settingsStore = SettingsStore(applicationContext)

            val primaryCandidate = resolveConnectCandidate(proxyStateStore)
            ensureGenerationCurrent(myGeneration)

            var candidates = listOf(primaryCandidate)
            var candidateIndex = 0
            var alternatesDiscovered = false

            fun activeCandidate(): ProxyCandidate = candidates[candidateIndex.coerceIn(0, candidates.lastIndex)]

            suspend fun rotateCandidate(): Boolean {
                if (candidateIndex + 1 >= candidates.size && !alternatesDiscovered) {
                    alternatesDiscovered = true
                    val fresh = discoverAlternateCandidates(primaryCandidate)
                        .filter { discovered -> candidates.none { known -> known.authority == discovered.authority } }
                        .take(MAX_ALTERNATE_EDGES)
                    if (fresh.isNotEmpty()) {
                        candidates = candidates + fresh
                        AppLogger.i(
                            TAG,
                            "found ${fresh.size} alternate edge(s) in ${primaryCandidate.countryCode} to fail over to",
                        )
                    }
                }
                if (candidateIndex + 1 >= candidates.size) return false
                candidateIndex++
                AppLogger.i(
                    TAG,
                    "failing over to edge ${activeCandidate().authority} " +
                        "(${candidateIndex + 1} of ${candidates.size})",
                )
                return true
            }

            val upstreamProxyConfig = if (settingsStore.upstreamProxyEnabled && settingsStore.upstreamProxyHost.isNotBlank()) {
                UpstreamProxyConfig(
                    type = when (settingsStore.upstreamProxyType) {
                        SettingsStore.UpstreamProxyType.SOCKS5 -> UpstreamProxyConfig.Type.SOCKS5
                        SettingsStore.UpstreamProxyType.HTTP -> UpstreamProxyConfig.Type.HTTP
                    },
                    host = settingsStore.upstreamProxyHost,
                    port = settingsStore.upstreamProxyPort,
                    username = settingsStore.upstreamProxyUsername.ifBlank { null },
                    password = settingsStore.upstreamProxyPassword.ifBlank { null },
                )
            } else {
                null
            }
            val chainSuffix = if (upstreamProxyConfig != null) {
                " via ${upstreamProxyConfig.type} proxy ${upstreamProxyConfig.host}:${upstreamProxyConfig.port}"
            } else {
                ""
            }

            val customEdgeAddress = settingsStore.effectiveCustomEdgeAddress
            val edgeSuffix = if (customEdgeAddress != null) {
                " via pinned edge $customEdgeAddress:${primaryCandidate.port}"
            } else {
                ""
            }

            val dohEndpointAddresses = settingsStore.effectiveBootstrapAddresses
            val bootstrapDesc = if (dohEndpointAddresses.isEmpty()) {
                "system DNS"
            } else {
                "${settingsStore.bootstrapDnsProvider.label} (${dohEndpointAddresses.joinToString(", ")})"
            }

            AppLogger.i(
                TAG,
                "connect: target=${primaryCandidate.authority} country=${primaryCandidate.countryCode} bootstrap=$bootstrapDesc$chainSuffix$edgeSuffix",
            )

            val socksPort = settingsStore.socksPort
            val socksBindAddress = settingsStore.socksBindAddress
            val httpProxyPort = settingsStore.httpProxyPort
            val httpProxyEnabled = settingsStore.httpProxyEnabled
            val proxyOnlyMode = settingsStore.proxyOnlyMode
            val excludedApps = settingsStore.excludedApps

            val dohUrl = settingsStore.effectiveDohUrl

            fun connectedLabel(): String = if (proxyOnlyMode) {
                if (httpProxyEnabled) {
                    "Proxy active \u2022 SOCKS $socksPort, HTTP $httpProxyPort"
                } else {
                    "Proxy active \u2022 $socksBindAddress:$socksPort"
                }
            } else {
                val target = activeCandidate()
                "Connected \u2022 ${target.countryName.ifBlank { target.countryCode }}"
            }

            if (proxyOnlyMode) {
                ControlPlaneHttp.socketProtector = null
            } else {
                ControlPlaneHttp.socketProtector = { socket -> protect(socket) }
            }

            val mapdnsActive = !proxyOnlyMode && dohUrl == null
            val socks = LocalSocks5Server(
                socksBindAddress,
                socksPort,
                mapdnsActive,
                dohUrl,
                { upstreamSession },
            ) {
                onUpstreamSessionUnhealthy()
            }
            socks.start()
            socksServer = socks

            if (httpProxyEnabled) {
                val http = LocalHttpProxyServer(
                    socksBindAddress,
                    httpProxyPort,
                    { upstreamSession },
                ) {
                    onUpstreamSessionUnhealthy()
                }
                http.start()
                httpProxyServer = http
            }
            ensureGenerationCurrent(myGeneration)

            if (proxyOnlyMode) {

                tunnelInterfaceActive = false
                AppLogger.i(
                    TAG,
                    "connect: proxy-only mode enabled, skipping TUN interface and tun2socks; " +
                        "local proxy at $socksBindAddress:$socksPort",
                )
            } else {
                val fd = establishTun(excludedApps, dohUrl) ?: error("Failed to establish TUN interface")
                tunFd = fd
                tunnelInterfaceActive = true

                reportedUnderlying = null
                runCatching {

                    firstUsableNetwork()?.let { underlying -> reportUnderlyingNetwork(underlying) }
                }

                val tunnelConfigPath = HevSocks5TunnelConfig.write(
                    applicationContext,
                    socksPort,
                    useMapDns = mapdnsActive,
                )
                val tunnelStarted = HevSocks5Tunnel.start(tunnelConfigPath, fd.fd)
                if (!tunnelStarted) error("Failed to start the tun2socks tunnel (hev-socks5-tunnel)")
                AppLogger.i(TAG, "connect: TUN interface up, hev-socks5-tunnel started")
                delay(POST_TUN_SETTLE_MS)
            }
            ensureGenerationCurrent(myGeneration)

            // Multi-account patch: one Guardian proxy pass + one H2 session per
            // enabled account, aggregated in a MultiUpstreamSessionPool. A
            // single account failing never aborts the whole connect.
            suspend fun dialUpstream(): MultiUpstreamSessionPool {
                val pool = MultiUpstreamSessionPool()
                var fatalFailure: Throwable? = null
                for (account in accounts) {
                    ensureGenerationCurrent(myGeneration)
                    val pass = try {
                        mintProxyPassFor(tokenStore, account)
                    } catch (failure: Throwable) {
                        if (failure is CancellationException) throw failure
                        if (isFatalUpstreamError(failure)) {
                            AppLogger.e(TAG, "fatal upstream error for ${account.email}; skipping the account", failure)
                            fatalFailure = fatalFailure ?: failure
                        } else {
                            AppLogger.w(TAG, "dial for ${account.email} failed: ${failure.message}")
                        }
                        tokenStore.updateAccount(account.copy(lastError = failure.message))
                        continue
                    }
                    val lifetimeNote = pass.expiresAtEpochSeconds?.let { expiresAt ->
                        val seconds = (expiresAt * 1_000L - System.currentTimeMillis()) / 1_000L
                        " (valid for ${seconds}s)"
                    } ?: " (lifetime not stated)"
                    AppLogger.i(TAG, "connect: acquired Guardian proxy pass for ${account.email}$lifetimeNote")

                    val target = activeCandidate()
                    val edgeAddress = customEdgeAddress
                        ?: resolveEdgeAddress(target.host, upstreamProxyConfig, dohEndpointAddresses)

                    val session = com.vauth.foxyvpn.vpn.upstream.H2UpstreamSession(
                        target.host,
                        target.port,
                        pass.token,
                        upstreamProxyConfig,
                        edgeAddress,
                    )
                    try {
                        session.connect()
                    } catch (failure: Throwable) {
                        runCatching { session.close() }
                        if (isFatalUpstreamError(failure)) {
                            AppLogger.e(TAG, "fatal tunnel error for ${account.email}; skipping the account", failure)
                            fatalFailure = fatalFailure ?: failure
                        } else {
                            AppLogger.w(TAG, "upstream tunnel for ${account.email} failed: ${failure.message}")
                        }
                        tokenStore.updateAccount(account.copy(lastError = failure.message))
                        continue
                    }
                    pool.addAccountSession(account.id, account.email, session)
                    pool.setPassExpiry(account.id, pass.expiresAtEpochSeconds)
                    tokenStore.updateAccount(account.copy(lastError = null))
                    AppLogger.i(
                        TAG,
                        "connect: upstream HTTP/2 tunnel established for ${account.email} to ${target.authority}",
                    )
                }
                if (pool.accountCount == 0) {
                    val fatal = fatalFailure
                    if (fatal != null) throw fatal
                    error("Could not reach a VPN server for any of the ${accounts.size} account(s).")
                }
                AppLogger.i(TAG, "connect: ${pool.accountCount} upstream account session(s) established")
                return pool
            }

            var dialAttempt = 0
            var lastDialFailure: Throwable? = null
            while (true) {
                dialAttempt++
                ensureGenerationCurrent(myGeneration)
                val target = activeCandidate()

                val dialResult = try {
                    withTimeout(CONNECT_TIMEOUT_MS) { dialUpstream() }
                } catch (cancellation: CancellationException) {

                    if (cancellation !is TimeoutCancellationException) throw cancellation
                    lastDialFailure = cancellation
                    AppLogger.w(TAG, "dial to ${target.authority} timed out (attempt $dialAttempt)")
                    null
                } catch (error: Throwable) {

                    if (isFatalUpstreamError(error)) throw error
                    lastDialFailure = error
                    AppLogger.w(TAG, "dial to ${target.authority} failed (attempt $dialAttempt): ${error.message}")
                    null
                }

                val session = dialResult

                if (session != null) {
                    if (myGeneration != connectionGeneration) {

                        runCatching { session.close() }
                        ensureGenerationCurrent(myGeneration)
                    }
                    upstreamSession = session
                    delay(INITIAL_DIAL_SETTLE_MS)
                    if (upstreamSession?.isConnected == true) break
                    AppLogger.w(
                        TAG,
                        "upstream tunnel to ${target.authority} died immediately after connecting " +
                            "(attempt $dialAttempt/$INITIAL_DIAL_MAX_ATTEMPTS)",
                    )
                    runCatching { upstreamSession?.close() }
                    upstreamSession = null

                    lastDialFailure = null
                }

                if (dialAttempt >= INITIAL_DIAL_MAX_ATTEMPTS) {

                    val (failures, cleared) = proxyStateStore.recordFailure()
                    if (cleared) {
                        AppLogger.w(
                            TAG,
                            "the saved location failed $failures connects in a row; clearing it so the next " +
                                "connect auto-selects a location",
                        )
                    }
                    val failure = lastDialFailure
                    if (failure != null) {
                        error(
                            "Could not reach a VPN server after $dialAttempt attempts across " +
                                "${candidates.size} server(s). Last error: ${friendlyErrorMessage(failure)}",
                        )
                    }
                    error(
                        "The VPN server closed the connection immediately, $dialAttempt times in a row. " +
                            "Try again shortly or pick a different location.",
                    )
                }

                rotateCandidate()
                delay(fullJitterBackoffMs(dialAttempt - 1, INITIAL_DIAL_BACKOFF_BASE_MS, INITIAL_DIAL_BACKOFF_CAP_MS))
            }

            ensureGenerationCurrent(myGeneration)

            val establishedCandidate = activeCandidate()

            if (establishedCandidate.authority == primaryCandidate.authority) {
                proxyStateStore.save(primaryCandidate)
            }

            val connectedText = connectedLabel()
            _state.value = ConnectionState.CONNECTED
            statusLabel = connectedText
            updateNotification(connectedText)
            AppLogger.i(TAG, "connect: CONNECTED via ${establishedCandidate.authority}")

            startMultiAccountProxyPassRenewal(myGeneration)

            // Multi-account patch: feed the pool's per-account stats into the
            // companion flows the UI observes.
            attachPoolStats()

            if (settingsStore.exitCheckEnabled) {
                scope.launch {
                    ExitCheck().verifyExitCountry(socksPort, establishedCandidate.countryCode)
                        .onSuccess { observed ->
                            if (myGeneration == connectionGeneration) {
                                AppLogger.i(TAG, "exit check: observed country=$observed")
                            }
                        }
                        .onFailure { AppLogger.w(TAG, "exit check failed (non-fatal)", it) }
                }
            }

            if (!proxyOnlyMode) {
                startSpeedUpdates()
            }

            watchdogJob = scope.launch {
                var consecutiveFailures = 0
                var waitingForNetwork = false

                suspend fun stopWithFatalError(message: String) {
                    AppLogger.e(TAG, "unrecoverable upstream failure; disconnecting: $message")
                    _lastError.value = message
                    connectionGeneration++
                    watchdogJob = null
                    _state.value = ConnectionState.DISCONNECTED
                    exitForeground()
                    releaseWakeLocks()
                    val doomed = detachResources()
                    opMutex.withLock { releaseResources(doomed, stopNativeTunnel = true) }
                    stopSelf()
                }

                while (isActive) {
                    delay(UPSTREAM_WATCHDOG_INTERVAL_MS)
                    val current = upstreamSession
                    if (current != null && current.isConnected) {
                        if (consecutiveFailures > 0 || waitingForNetwork) {
                            AppLogger.i(TAG, "upstream tunnel is healthy again")
                            _lastError.value = null
                            statusLabel = connectedLabel()
                            updateNotification(statusLabel)
                        }
                        consecutiveFailures = 0
                        waitingForNetwork = false
                        continue
                    }

                    if (!hasUsableNetwork()) {
                        if (!waitingForNetwork) {
                            AppLogger.i(TAG, "no usable network; holding the session and waiting for connectivity")
                            waitingForNetwork = true
                            statusLabel = "Waiting for network\u2026"
                            updateNotification(statusLabel)
                        }
                        consecutiveFailures = 0
                        continue
                    }
                    if (waitingForNetwork) {
                        AppLogger.i(TAG, "network is back; redialing the upstream tunnel")
                        waitingForNetwork = false
                    }

                    AppLogger.w(TAG, "upstream tunnel is down; attempting to reconnect")
                    runCatching { current?.close() }
                    statusLabel = "Reconnecting\u2026"
                    updateNotification(statusLabel)

                    val dialResult = try {
                        withTimeout(CONNECT_TIMEOUT_MS) { dialUpstream() }
                    } catch (cancellation: CancellationException) {
                        if (cancellation !is TimeoutCancellationException) throw cancellation
                        consecutiveFailures++
                        AppLogger.w(TAG, "upstream reconnect attempt $consecutiveFailures timed out")
                        null
                    } catch (error: Throwable) {
                        if (isFatalUpstreamError(error)) {
                            stopWithFatalError(friendlyErrorMessage(error))
                            return@launch
                        }
                        consecutiveFailures++
                        AppLogger.w(TAG, "upstream reconnect attempt $consecutiveFailures failed: ${error.message}")
                        null
                    }
                    val fresh = dialResult

                    if (fresh == null) {

                        if (consecutiveFailures % RECONNECT_FAILURES_BEFORE_EDGE_ROTATION == 0) {
                            rotateCandidate()
                        }

                        if (consecutiveFailures == RECONNECT_FAILURES_BEFORE_WARNING) {
                            _lastError.value =
                                "Still trying to reconnect to the VPN server. Tap Disconnect to stop."
                            AppLogger.e(
                                TAG,
                                "$consecutiveFailures consecutive reconnect failures; continuing to retry with backoff " +
                                    "(capped at ${RECONNECT_BACKOFF_CAP_MS / 1_000}s between attempts)",
                            )
                        }
                        delay(fullJitterBackoffMs(consecutiveFailures - 1, RECONNECT_BACKOFF_BASE_MS, RECONNECT_BACKOFF_CAP_MS))
                        continue
                    }

                    if (myGeneration != connectionGeneration) {

                        AppLogger.i(TAG, "discarding stale reconnect from a torn-down session generation")
                        runCatching { fresh.close() }
                        return@launch
                    }
                    upstreamSession = fresh
                    consecutiveFailures = 0
                    _lastError.value = null

                    statusLabel = connectedLabel()
                    updateNotification(statusLabel)
                    AppLogger.i(TAG, "upstream tunnel reconnected via ${activeCandidate().authority}")

                    startMultiAccountProxyPassRenewal(myGeneration)
                    attachPoolStats()
                }
            }
        }.onFailure { failure ->

            if (failure is CancellationException) {
                AppLogger.i(TAG, "connect: aborted because the connection was cancelled")
                releaseResources(detachResources(), stopNativeTunnel = true)
                return@onFailure
            }
            val message = if (failure is TimeoutCancellationException) {
                "Connection timed out. Check your network and try again."
            } else {
                friendlyErrorMessage(failure)
            }
            AppLogger.e(TAG, "connect failed", failure)
            _lastError.value = message
            teardown()
            exitForeground()
            releaseWakeLocks()
            stopSelf()
        }
        connectJob = null
    }

    /**
     * Multi-account patch: attaches the current pool's stats tracker and
     * mirrors its per-account stats into the companion flows. Re-attached
     * after every watchdog redial, which swaps in a fresh pool.
     */
    private fun attachPoolStats() {
        poolStatsJob?.cancel()
        val pool = upstreamSession as? MultiUpstreamSessionPool ?: return
        lastPersistedBytes = emptyMap()
        lastPersistAt = 0L
        poolStatsJob = scope.launch {
            pool.startStatsTracker(this)
            pool.liveStats.collect { stats ->
                _multiAccountStats.value = stats
                _connectedAccountCount.value = stats.count { it.isConnected && !it.isExhausted }
                // Lightweight cumulative usage persistence (Bug 2): the pool
                // counters reset on every redial, so accumulate deltas here.
                // v1.3 lag fix: EncryptedSharedPreferences writes are slow —
                // persist at most every 30s instead of on every 2s tick.
                val now = System.currentTimeMillis()
                val tokenStore = (applicationContext as? FoxyVpnApp)?.tokenStore
                if (tokenStore != null && now - lastPersistAt >= USAGE_PERSIST_INTERVAL_MS) {
                    lastPersistAt = now
                    val prev = lastPersistedBytes
                    for (s in stats) {
                        val total = s.bytesDown + s.bytesUp
                        val p = prev[s.accountId] ?: 0L
                        if (total > p) tokenStore.addUsedBytes(s.accountId, total - p)
                    }
                    lastPersistedBytes = stats.associate { it.accountId to (it.bytesDown + it.bytesUp) }
                }
            }
        }
    }

    private suspend fun resolveConnectCandidate(proxyStateStore: ProxyStateStore): ProxyCandidate {
        proxyStateStore.load()?.let { return it }
        AppLogger.i(TAG, "connect: no server previously selected; auto-selecting recommended location")
        val client = (applicationContext as? FoxyVpnApp)?.serverListClient ?: ServerListClient(applicationContext)
        val countries = runCatching {
            withTimeout(SERVER_LIST_FETCH_TIMEOUT_MS) { client.fetchCountries() }
        }.getOrElse {
            client.loadCachedCountries()
        }
        val recommended = ServerListClient.candidatesForCountry(countries, RECOMMENDED_COUNTRY_CODE).randomOrNull()
        val chosen = recommended ?: countries.firstOrNull { it.code.isNotBlank() }?.let { country ->
            ServerListClient.candidatesForCountry(countries, country.code).randomOrNull()
        } ?: error("No server selected and no servers are available. Choose a location first.")
        proxyStateStore.save(chosen)
        AppLogger.i(TAG, "connect: auto-selected ${chosen.authority} country=${chosen.countryCode}")
        return chosen
    }

    private suspend fun discoverAlternateCandidates(primary: ProxyCandidate): List<ProxyCandidate> {
        val attempt = runCatching {
            val client = (applicationContext as? FoxyVpnApp)?.serverListClient ?: ServerListClient(applicationContext)
            val countries = runCatching {
                withTimeout(SERVER_LIST_FETCH_TIMEOUT_MS) { client.fetchCountries() }
            }.getOrElse {
                client.loadCachedCountries()
            }
            val sameCity = ServerListClient.candidatesForCity(countries, primary.countryCode, primary.cityCode)
            val sameCountry = ServerListClient.candidatesForCountry(countries, primary.countryCode)
            (sameCity + sameCountry)
                .distinctBy { it.authority }
                .filter { it.authority != primary.authority }
        }
        val failure = attempt.exceptionOrNull()
        if (failure is CancellationException && failure !is TimeoutCancellationException) throw failure
        if (failure != null) {
            AppLogger.w(TAG, "could not fetch alternate edges; staying with ${primary.authority}", failure)
            return emptyList()
        }
        return attempt.getOrDefault(emptyList())
    }

    private suspend fun resolveEdgeAddress(
        host: String,
        upstreamProxy: UpstreamProxyConfig?,
        dohEndpointAddresses: List<String>,
    ): String? {
        if (upstreamProxy != null) return null
        if (dohEndpointAddresses.isEmpty()) {
            AppLogger.d(TAG, "resolving $host via system DNS resolver (direct network)")
            return null
        }
        return EdgeAddressResolver.resolve(host, dohEndpointAddresses)
    }

    private fun friendlyErrorMessage(error: Throwable): String = when (error) {
        is QuotaExceededError -> "Your VPN quota is exhausted. Try again later."
        is TokenInvalidError -> "Your session was rejected. Please sign in again."
        is TimeoutCancellationException -> "the connection timed out"
        else -> {
            val untrusted = generateSequence(error) { it.cause }.take(8).any {
                it is java.security.cert.CertPathValidatorException || it is java.security.cert.CertificateException
            }
            val base = error.message ?: (error::class.simpleName ?: "Connection failed")
            if (untrusted) {
                "$base (this device does not trust the VPN server's certificate; updating Android or the device's date/time may help)"
            } else {
                base
            }
        }
    }

    private fun establishTun(excludedApps: Set<String>, dohUrl: String?): ParcelFileDescriptor? {
        val dnsServer = HevSocks5TunnelConfig.MAPDNS_ADDRESS
        if (dohUrl != null) {
            AppLogger.i(TAG, "establishTun: dns=DoH ($dohUrl)")
        } else {
            AppLogger.i(TAG, "establishTun: dns=mapdns ($dnsServer)")
        }
        return Builder()
            .setSession("FoxyVPN")
            .addAddress(HevSocks5TunnelConfig.TUN_ADDRESS, 32)
            .addRoute("0.0.0.0", 0)
            .apply {

                val ipv6Address = runCatching {
                    addAddress(TUN_ADDRESS_IPV6, TUN_PREFIX_LENGTH_IPV6)
                }.onFailure {
                    AppLogger.w(
                        TAG,
                        "could not add an IPv6 address to the TUN interface; IPv6 will be captured by the " +
                            "tunnel's default route and dropped rather than sent in the clear",
                        it,
                    )
                }

                runCatching { addRoute("::", 0) }.onFailure {
                    AppLogger.e(
                        TAG,
                        "could not claim the IPv6 default route; IPv6 traffic may bypass the tunnel and leak " +
                            "the device's real address",
                        it,
                    )
                }
                if (ipv6Address.isFailure) {
                    AppLogger.i(TAG, "establishTun: IPv6 is routed into the tunnel and blackholed (no IPv6 address)")
                }
            }
            .addDnsServer(dnsServer)

            .setMtu(HevSocks5TunnelConfig.TUN_MTU)
            .setBlocking(true)
            .apply {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    runCatching { setMetered(false) }
                        .onFailure { AppLogger.d(TAG, "could not mark the tunnel as unmetered: ${it.message}") }
                }
            }
            .apply {
                runCatching { addDisallowedApplication(packageName) }
                    .onFailure { AppLogger.w(TAG, "failed to exclude own app from the VPN tunnel", it) }
                for (excludedPackage in excludedApps) {
                    runCatching { addDisallowedApplication(excludedPackage) }
                        .onFailure { AppLogger.w(TAG, "failed to exclude $excludedPackage from the VPN tunnel (app may be uninstalled)", it) }
                }
            }
            .establish()
    }

    private fun teardown() {
        connectionGeneration++
        watchdogJob?.cancel()
        watchdogJob = null
        releaseResources(detachResources(), stopNativeTunnel = true)
        _state.value = ConnectionState.DISCONNECTED
    }

    private fun detachResources(): SessionResources {
        speedJob?.cancel()
        speedJob = null

        tokenRenewalJob?.cancel()
        tokenRenewalJob = null

        poolStatsJob?.cancel()
        poolStatsJob = null
        val detached = SessionResources(tunFd, socksServer, httpProxyServer, upstreamSession)
        tunFd = null
        tunnelInterfaceActive = false
        socksServer = null
        httpProxyServer = null
        upstreamSession = null
        return detached
    }

    private fun releaseResources(resources: SessionResources, stopNativeTunnel: Boolean) {
        if (resources.isEmpty && !stopNativeTunnel) return
        if (stopNativeTunnel) {
            runCatching { HevSocks5Tunnel.stop() }.onFailure { AppLogger.w(TAG, "error stopping tun2socks tunnel", it) }
        }
        runCatching { resources.socksServer?.stop() }.onFailure { AppLogger.w(TAG, "error stopping local SOCKS5 server", it) }
        runCatching { resources.httpProxyServer?.stop() }.onFailure { AppLogger.w(TAG, "error stopping local HTTP proxy server", it) }
        runCatching { resources.upstreamSession?.close() }.onFailure { AppLogger.w(TAG, "error closing upstream session", it) }
        runCatching { resources.tunFd?.close() }.onFailure { AppLogger.w(TAG, "error closing TUN interface", it) }
    }

    override fun onRevoke() {
        requestDisconnect("VPN permission was revoked by the system")
        super.onRevoke()
    }

    private fun startSpeedUpdates() {
        speedJob?.cancel()
        speedJob = scope.launch {
            var lastStats = HevSocks5Tunnel.stats()
            var lastSampleAt = System.currentTimeMillis()
            while (isActive) {
                delay(SPEED_UPDATE_INTERVAL_MS)
                val stats = HevSocks5Tunnel.stats()
                val now = System.currentTimeMillis()
                val elapsedSeconds = ((now - lastSampleAt).coerceAtLeast(1)).toDouble() / 1_000.0

                val txRate = ((stats[1] - lastStats[1]).coerceAtLeast(0) / elapsedSeconds).toLong()
                val rxRate = ((stats[3] - lastStats[3]).coerceAtLeast(0) / elapsedSeconds).toLong()
                lastStats = stats
                lastSampleAt = now
                if (_state.value != ConnectionState.CONNECTED) continue
                updateNotification(
                    statusLabel,
                    "\u2193 ${formatBytesPerSecond(rxRate)}  \u2191 ${formatBytesPerSecond(txRate)}",
                )
            }
        }
    }

    private fun buildNotification(text: String, subText: String? = null): Notification {
        val openIntent = PendingIntent.getActivity(
            this, REQUEST_CODE_OPEN, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val disconnectIntent = PendingIntent.getService(
            this, REQUEST_CODE_DISCONNECT, Intent(this, FoxyVpnService::class.java).setAction(ACTION_DISCONNECT),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val builder = NotificationCompat.Builder(this, FoxyVpnApp.VPN_NOTIFICATION_CHANNEL_ID)
            .setContentTitle("FoxyVPN")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(openIntent)
            .setOngoing(true)

            .setOnlyAlertOnce(true)

            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)

            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

            .setLocalOnly(true)

            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Disconnect", disconnectIntent)
        if (subText != null) {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText("$text\n$subText"))
        }
        return builder.build()
    }

    private fun enterForeground(text: String) {
        foregroundActive = true
        lastNotificationText = text
        startForeground(NOTIFICATION_ID, buildNotification(text))
    }

    private fun exitForeground() {
        foregroundActive = false
        lastNotificationText = null
        runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
    }

    private fun updateNotification(text: String, subText: String? = null) {
        if (!foregroundActive) return
        val key = if (subText == null) text else "$text\n$subText"
        if (key == lastNotificationText) return
        lastNotificationText = key
        val manager = getSystemService(NotificationManager::class.java) ?: return
        runCatching { manager.notify(NOTIFICATION_ID, buildNotification(text, subText)) }
    }

    companion object {
        const val ACTION_CONNECT = "com.vauth.foxyvpn.action.CONNECT"
        const val ACTION_DISCONNECT = "com.vauth.foxyvpn.action.DISCONNECT"
        private const val NOTIFICATION_ID = 1
        private const val REQUEST_CODE_OPEN = 0
        private const val REQUEST_CODE_DISCONNECT = 1

        private val _state = MutableStateFlow(ConnectionState.DISCONNECTED)
        val state: StateFlow<ConnectionState> = _state

        private val _lastError = MutableStateFlow<String?>(null)
        val lastError: StateFlow<String?> = _lastError

        // Multi-account patch: per-account live stats and connected count,
        // fed by the MultiUpstreamSessionPool's stats tracker.
        private val _multiAccountStats = MutableStateFlow<List<AccountLiveStats>>(emptyList())
        val multiAccountStats: StateFlow<List<AccountLiveStats>> = _multiAccountStats

        private val _connectedAccountCount = MutableStateFlow(0)
        val connectedAccountCount: StateFlow<Int> = _connectedAccountCount

        fun start(context: Context) {
            context.startForegroundService(Intent(context, FoxyVpnService::class.java).setAction(ACTION_CONNECT))
        }

        fun stop(context: Context) {

            val intent = Intent(context, FoxyVpnService::class.java).setAction(ACTION_DISCONNECT)
            runCatching { context.startService(intent) }
                .onFailure {
                    AppLogger.w(TAG, "could not deliver the disconnect intent; the service is not running", it)
                }
        }
    }
}
