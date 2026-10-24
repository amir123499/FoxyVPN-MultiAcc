package com.vauth.foxyvpn.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import com.vauth.foxyvpn.ui.i18n.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vauth.foxyvpn.FoxyVpnApp
import com.vauth.foxyvpn.data.SettingsStore
import com.vauth.foxyvpn.data.formatBytesPerSecond
import com.vauth.foxyvpn.data.model.AccountLiveStats
import com.vauth.foxyvpn.data.model.ConnectionState
import com.vauth.foxyvpn.data.model.ProxyCandidate
import com.vauth.foxyvpn.data.model.VpnAccount
import com.vauth.foxyvpn.data.model.emojiFlag
import com.vauth.foxyvpn.ui.theme.AppearanceController
import com.vauth.foxyvpn.ui.theme.CardCornerStyle
import com.vauth.foxyvpn.ui.theme.ListDensity
import com.vauth.foxyvpn.ui.theme.LocalFoxyStatusColors
import com.vauth.foxyvpn.ui.theme.Personalization
import com.vauth.foxyvpn.ui.theme.ThemeMode
import com.vauth.foxyvpn.vpn.FoxyVpnService
import com.vauth.foxyvpn.vpn.PingUtil
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    app: FoxyVpnApp,
    appearance: AppearanceController,
    onRequestConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onOpenServers: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAccounts: () -> Unit,
) {
    val state by FoxyVpnService.state.collectAsState()
    val lastError by FoxyVpnService.lastError.collectAsState()
    // NOTE (v1.3 lag fix): multiAccountStats emits every 2s while connected —
    // it is collected locally inside SpeedRow/DetailsPanel, never here.
    val connectedCount by FoxyVpnService.connectedAccountCount.collectAsState()
    val accounts by app.tokenStore.accountsFlow.collectAsState()
    val selectedProxy by app.proxyStateStore.selectedProxyFlow.collectAsState()

    val haptics = LocalHapticFeedback.current
    var detailsOpen by remember { mutableStateOf(false) }
    var pingMs by remember { mutableIntStateOf(-1) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(detailsOpen, state) {
        if (detailsOpen && state == ConnectionState.CONNECTED) {
            val proxy = selectedProxy
            pingMs = -1
            if (proxy != null) {
                pingMs = PingUtil.measureTcpLatencyMs(proxy.host, proxy.port) ?: -2
            }
        }
    }

    // NOTE (v1.3 lag fix): the elapsed-seconds ticker used to live here and
    // recomposed the ENTIRE home screen every second while connected. It now
    // lives inside the connect core (rememberElapsedSeconds) so only the
    // elapsed Text recomposes.

    fun navigate(dest: FoxyDestination) {
        when (dest) {
            FoxyDestination.Home -> Unit
            FoxyDestination.Locations -> onOpenServers()
            FoxyDestination.Accounts -> onOpenAccounts()
            FoxyDestination.Settings -> onOpenSettings()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FoxyVPN", style = MaterialTheme.typography.titleLargeEmphasized) },
                actions = {
                    IconButton(
                        onClick = {
                            if (appearance.haptics) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            val next = when (appearance.themeMode) {
                                ThemeMode.LIGHT -> ThemeMode.DARK
                                ThemeMode.DARK -> ThemeMode.AMOLED
                                ThemeMode.AMOLED -> ThemeMode.LIGHT
                                ThemeMode.SYSTEM -> ThemeMode.DARK
                            }
                            appearance.updateThemeMode(next)
                        },
                    ) {
                        Icon(
                            imageVector = when (appearance.themeMode) {
                                ThemeMode.SYSTEM -> Icons.Filled.BrightnessAuto
                                ThemeMode.LIGHT -> Icons.Filled.LightMode
                                ThemeMode.DARK -> Icons.Filled.DarkMode
                                ThemeMode.AMOLED -> Icons.Filled.Contrast
                            },
                            contentDescription = "Theme (tap to cycle)",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
        bottomBar = {
            FoxyBottomBar(
                style = appearance.navBarStyle,
                current = FoxyDestination.Home,
                onNavigate = ::navigate,
            )
        },
    ) { padding ->
        val data = HomeContentData(
            state = state,
            lastError = lastError,
            connectedCount = connectedCount,
            accounts = accounts,
            selectedProxy = selectedProxy,
            pingMs = pingMs,
            detailsOpen = detailsOpen,
            appearance = appearance,
        )
        val callbacks = HomeCallbacks(
            onToggle = {
                if (state == ConnectionState.DISCONNECTED) onRequestConnect() else onDisconnect()
            },
            onToggleDetails = { detailsOpen = !detailsOpen },
            onRefreshPing = {
                val proxy = selectedProxy
                scope.launch {
                    pingMs = -1
                    pingMs = proxy?.let { PingUtil.measureTcpLatencyMs(it.host, it.port) } ?: -2
                }
            },
            onOpenServers = onOpenServers,
            onOpenAccounts = onOpenAccounts,
        )
        when (appearance.appDesign) {
            SettingsStore.AppDesign.PIXEL -> PixelHomeBody(padding, data, callbacks)
            SettingsStore.AppDesign.PORTAL -> PortalHomeBody(padding, data, callbacks)
            SettingsStore.AppDesign.OLD -> OldHomeBody(padding, data, callbacks)
        }
    }
}

private data class HomeContentData(
    val state: ConnectionState,
    val lastError: String?,
    // NOTE (v1.3 lag fix): multiStats is NOT collected here anymore. The pool
    // emits fresh stats every 2s while connected; collecting at HomeScreen
    // level recomposed the whole tree on every tick. Consumers (SpeedRow,
    // DetailsPanel) collect it locally so only they recompose.
    val connectedCount: Int,
    val accounts: List<VpnAccount>,
    val selectedProxy: ProxyCandidate?,
    val pingMs: Int,
    val detailsOpen: Boolean,
    val appearance: AppearanceController,
)

private class HomeCallbacks(
    val onToggle: () -> Unit,
    val onToggleDetails: () -> Unit,
    val onRefreshPing: () -> Unit,
    val onOpenServers: () -> Unit,
    val onOpenAccounts: () -> Unit,
)

private val HomeContentData.corner: Dp
    get() = appearance.personalization.cornerStyle.cardDp.dp

private val HomeContentData.compact: Boolean
    get() = appearance.personalization.density == ListDensity.Compact

private fun HomeContentData.cardShape() = RoundedCornerShape(corner)

@Composable
private fun HomeConnectCore(data: HomeContentData, callbacks: HomeCallbacks) {
    val a = data.appearance
    ConnectCore(
        state = data.state,
        style = a.connectButtonStyle,
        reduceMotion = a.reduceMotion,
        connectColor = a.personalization.connectColor,
        connectedColor = a.personalization.connectedColor,
        haptics = a.haptics,
        onToggle = callbacks.onToggle,
    )
}

/**
 * Scoped 1-second elapsed ticker (v1.3 lag fix): only this Text recomposes
 * each second while connected — never the parent screen.
 */
@Composable
private fun ElapsedText(
    state: ConnectionState,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier: Modifier = Modifier,
    prefix: String = "",
) {
    var elapsed by remember { mutableIntStateOf(0) }
    LaunchedEffect(state) {
        if (state == ConnectionState.CONNECTED) {
            val start = System.currentTimeMillis()
            while (true) {
                kotlinx.coroutines.delay(1_000L)
                elapsed = ((System.currentTimeMillis() - start) / 1_000L).toInt()
            }
        } else {
            elapsed = 0
        }
    }
    if (state == ConnectionState.CONNECTED) {
        Text(
            text = "$prefix${formatElapsed(elapsed)}",
            style = style,
            color = color,
            modifier = modifier,
        )
    }
}

@Composable
private fun PixelHomeBody(
    padding: PaddingValues,
    data: HomeContentData,
    callbacks: HomeCallbacks,
) {
    val state = data.state
    val vGap = if (data.compact) 6.dp else 10.dp
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .widthIn(max = 560.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(8.dp))

        HeroCard(data = data, callbacks = callbacks)

        Spacer(Modifier.height(vGap))

        FactChipsRow(
            state = state,
            placeLabel = data.selectedProxy?.let { it.countryName.ifBlank { it.countryCode } } ?: "Recommended",
            flag = data.selectedProxy?.emojiFlag.orEmpty(),
            pingMs = data.pingMs,
            connectedCount = data.connectedCount,
            totalAccounts = data.accounts.size,
            onOpenDetails = callbacks.onToggleDetails,
        )

        AnimatedVisibility(visible = data.detailsOpen && state == ConnectionState.CONNECTED) {
            DetailsPanel(
                accounts = data.accounts,
                pingMs = data.pingMs,
                personalization = data.appearance.personalization,
                placeLabel = data.selectedProxy?.let {
                    listOfNotNull(it.countryName.ifBlank { null }, it.host).joinToString(" · ")
                } ?: "Recommended",
                onRefreshPing = callbacks.onRefreshPing,
            )
        }

        val error = data.lastError
        if (error != null && state != ConnectionState.CONNECTING) {
            Spacer(Modifier.height(vGap))
            ErrorCard(error = error, corner = data.corner)
        }

        Spacer(Modifier.height(vGap))

        ServerRow(
            flag = data.selectedProxy?.emojiFlag.orEmpty(),
            name = data.selectedProxy?.let { it.countryName.ifBlank { it.countryCode } } ?: "Recommended",
            detail = data.selectedProxy?.let { "${it.host}:${it.port}" } ?: "Tap to choose a location",
            personalization = data.appearance.personalization,
            corner = data.corner,
            onClick = callbacks.onOpenServers,
        )

        Spacer(Modifier.height(vGap))

        AccountsRow(
            accounts = data.accounts,
            connectedCount = data.connectedCount,
            state = state,
            personalization = data.appearance.personalization,
            corner = data.corner,
            onClick = callbacks.onOpenAccounts,
        )

        Spacer(Modifier.height(vGap))

        SpeedRow(live = state == ConnectionState.CONNECTED, corner = data.corner)

        Spacer(Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HeroCard(
    data: HomeContentData,
    callbacks: HomeCallbacks,
) {
    val cs = MaterialTheme.colorScheme
    val state = data.state
    val live = state == ConnectionState.CONNECTED
    val busy = state == ConnectionState.CONNECTING
    // Card colours animate with Material 3 slow-effects motion; 44.dp corners.
    val effects = MaterialTheme.motionScheme.slowEffectsSpec<Color>()
    val container by animateColorAsState(
        if (live) cs.primaryContainer else cs.surfaceContainer,
        effects,
        label = "pixel-card",
    )
    val ink by animateColorAsState(
        if (live) cs.onPrimaryContainer else cs.onSurface,
        effects,
        label = "pixel-ink",
    )
    val sub by animateColorAsState(
        if (live) cs.onPrimaryContainer.copy(alpha = 0.78f) else cs.onSurfaceVariant,
        effects,
        label = "pixel-sub",
    )
    val rise = MaterialTheme.motionScheme.defaultSpatialSpec<androidx.compose.ui.unit.IntOffset>()

    Surface(
        shape = RoundedCornerShape(44.dp),
        color = container,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HomeConnectCore(data = data, callbacks = callbacks)
            Spacer(Modifier.height(16.dp))
            // Crossfade with a short upward slide when the content changes.
            AnimatedContent(
                targetState = state,
                transitionSpec = {
                    (fadeIn(tween(280)) + slideInVertically(rise) { it / 3 }) togetherWith fadeOut(tween(160))
                },
                label = "pixel-state",
            ) { s ->
                Text(
                    text = when (s) {
                        ConnectionState.CONNECTED -> "Connected"
                        ConnectionState.CONNECTING -> "Connecting…"
                        ConnectionState.DISCONNECTED -> "Disconnected"
                    },
                    style = MaterialTheme.typography.headlineSmallEmphasized,
                    color = ink,
                    textAlign = TextAlign.Center,
                )
            }
            if (busy) {
                Spacer(Modifier.height(10.dp))
                // Wavy linear progress indicator, 156.dp wide.
                LinearWavyProgressIndicator(
                    modifier = Modifier.width(156.dp),
                    color = cs.primary,
                    trackColor = cs.primary.copy(alpha = 0.18f),
                )
                Spacer(Modifier.height(6.dp))
                Text("Tap the core to cancel", style = MaterialTheme.typography.bodySmall, color = sub)
            } else if (live) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "All traffic is protected",
                    style = MaterialTheme.typography.bodySmall,
                    color = sub,
                )
            } else {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Tap the core to connect",
                    style = MaterialTheme.typography.bodySmall,
                    color = sub,
                )
            }
        }
    }
}

/**
 * PORTAL design: immersive single column. The connect core glows at the
 * center; facts live in one unified card below.
 */
@Composable
private fun PortalHomeBody(
    padding: PaddingValues,
    data: HomeContentData,
    callbacks: HomeCallbacks,
) {
    val cs = MaterialTheme.colorScheme
    val state = data.state
    val live = state == ConnectionState.CONNECTED
    val busy = state == ConnectionState.CONNECTING
    val selectedProxy = data.selectedProxy
    val vGap = if (data.compact) 8.dp else 12.dp

    val ring by animateColorAsState(
        targetValue = when {
            live -> cs.primary
            busy -> cs.tertiary
            else -> cs.surfaceContainerHighest
        },
        animationSpec = tween(500),
        label = "portal-ring",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .widthIn(max = 560.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(12.dp))
        AnimatedContent(
            targetState = state,
            transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(150)) },
            label = "portal-title",
        ) { s ->
            Text(
                text = when (s) {
                    ConnectionState.CONNECTED -> "Connected"
                    ConnectionState.CONNECTING -> "Connecting…"
                    ConnectionState.DISCONNECTED -> "Disconnected"
                },
                style = MaterialTheme.typography.headlineSmallEmphasized,
                color = if (live) cs.primary else cs.onSurface,
            )
        }
        if (live) {
            ElapsedText(
                state = state,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Spacer(Modifier.height(20.dp))

        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(236.dp)
                    .clip(CircleShape)
                    .background(ring.copy(alpha = 0.14f)),
            )
            Box(
                modifier = Modifier
                    .size(208.dp)
                    .clip(CircleShape)
                    .background(ring.copy(alpha = 0.20f)),
            )
            HomeConnectCore(data = data, callbacks = callbacks)
        }

        Spacer(Modifier.height(20.dp))

        Surface(
            shape = RoundedCornerShape(data.corner),
            color = cs.surfaceContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(vertical = 6.dp)) {
                PortalInfoRow(
                    label = "Location",
                    value = selectedProxy?.let { it.countryName.ifBlank { it.countryCode } } ?: "Recommended",
                    flag = selectedProxy?.emojiFlag.orEmpty(),
                )
                HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = cs.outlineVariant.copy(alpha = 0.5f))
                PortalInfoRow(
                    label = "Ping",
                    value = when {
                        data.pingMs < 0 -> "—"
                        else -> "${data.pingMs} ms"
                    },
                    flag = "",
                )
                HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = cs.outlineVariant.copy(alpha = 0.5f))
                PortalInfoRow(
                    label = "Accounts",
                    value = "${data.connectedCount}/${data.accounts.size} active",
                    flag = "",
                )
            }
        }

        val error = data.lastError
        if (error != null && state != ConnectionState.CONNECTING) {
            Spacer(Modifier.height(vGap))
            ErrorCard(error = error, corner = data.corner)
        }

        Spacer(Modifier.height(vGap))
        ServerRow(
            flag = selectedProxy?.emojiFlag.orEmpty(),
            name = selectedProxy?.let { it.countryName.ifBlank { it.countryCode } } ?: "Recommended",
            detail = selectedProxy?.let { "${it.host}:${it.port}" } ?: "Tap to choose a location",
            personalization = data.appearance.personalization,
            corner = data.corner,
            onClick = callbacks.onOpenServers,
        )
        Spacer(Modifier.height(vGap))
        AccountsRow(
            accounts = data.accounts,
            connectedCount = data.connectedCount,
            state = state,
            personalization = data.appearance.personalization,
            corner = data.corner,
            onClick = callbacks.onOpenAccounts,
        )
        Spacer(Modifier.height(vGap))
        SpeedRow(live = live, corner = data.corner)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun PortalInfoRow(label: String, value: String, flag: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        if (flag.isNotBlank()) {
            Text(text = flag, modifier = Modifier.padding(end = 6.dp))
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmallEmphasized,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * CLASSIC design: traditional VPN-app layout — big button, status line,
 * plain divided rows.
 */
@Composable
private fun OldHomeBody(
    padding: PaddingValues,
    data: HomeContentData,
    callbacks: HomeCallbacks,
) {
    val cs = MaterialTheme.colorScheme
    val state = data.state
    val live = state == ConnectionState.CONNECTED
    val selectedProxy = data.selectedProxy

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .widthIn(max = 560.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        AnimatedContent(
            targetState = state,
            transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(150)) },
            label = "classic-title",
        ) { s ->
            Text(
                text = when (s) {
                    ConnectionState.CONNECTED -> "VPN is ON"
                    ConnectionState.CONNECTING -> "Connecting…"
                    ConnectionState.DISCONNECTED -> "VPN is OFF"
                },
                style = MaterialTheme.typography.headlineMedium,
                color = if (live) cs.primary else cs.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(18.dp))
        HomeConnectCore(data = data, callbacks = callbacks)
        if (live) {
            Spacer(Modifier.height(10.dp))
            ElapsedText(
                state = state,
                style = MaterialTheme.typography.bodyMedium,
                prefix = "Connected for ",
            )
        }
        Spacer(Modifier.height(24.dp))

        val error = data.lastError
        if (error != null && state != ConnectionState.CONNECTING) {
            ErrorCard(error = error, corner = data.corner)
            Spacer(Modifier.height(12.dp))
        }

        ClassicRow(
            title = "Server",
            subtitle = selectedProxy?.let {
                "${it.countryName.ifBlank { it.countryCode }} · ${it.host}:${it.port}"
            } ?: "Recommended — tap to choose",
            monospace = data.appearance.personalization.monospaceAddress,
            onClick = callbacks.onOpenServers,
        )
        HorizontalDivider(color = cs.outlineVariant.copy(alpha = 0.6f))
        ClassicRow(
            title = "Accounts",
            subtitle = "${data.connectedCount} of ${data.accounts.size} connected",
            monospace = false,
            onClick = callbacks.onOpenAccounts,
        )
        HorizontalDivider(color = cs.outlineVariant.copy(alpha = 0.6f))
        ClassicRow(
            title = "Speed",
            subtitle = if (live) "Live" else "Idle",
            monospace = false,
            onClick = {},
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ClassicRow(title: String, subtitle: String, monospace: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

fun formatElapsed(totalSeconds: Int): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FactChipsRow(
    state: ConnectionState,
    placeLabel: String,
    flag: String,
    pingMs: Int,
    connectedCount: Int,
    totalAccounts: Int,
    onOpenDetails: () -> Unit,
) {
    val live = state == ConnectionState.CONNECTED
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        modifier = Modifier.fillMaxWidth(),
    ) {
        FactChip(onClick = onOpenDetails) {
            if (flag.isNotBlank()) {
                Text(flag, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.width(6.dp))
            } else {
                Icon(Icons.Filled.Public, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(placeLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (live) {
            FactChip(onClick = onOpenDetails) {
                Text(
                    when (pingMs) {
                        -1 -> "ping …"
                        -2 -> "ping n/a"
                        else -> "$pingMs ms"
                    },
                    maxLines = 1,
                )
            }
            FactChip(onClick = onOpenDetails) {
                Icon(Icons.Filled.AccountCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("$connectedCount/$totalAccounts", maxLines = 1)
            }
        }
    }
}

@Composable
private fun FactChip(onClick: () -> Unit, content: @Composable () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = cs.surfaceContainerHigh,
        contentColor = cs.onSurfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalTextStyle provides MaterialTheme.typography.labelLarge,
            ) {
                content()
            }
        }
    }
}

@Composable
private fun DetailsPanel(
    accounts: List<VpnAccount>,
    pingMs: Int,
    personalization: Personalization,
    placeLabel: String,
    onRefreshPing: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    // v1.3 lag fix: collected here so stats ticks recompose only this panel.
    val stats by FoxyVpnService.multiAccountStats.collectAsState()
    val byId = remember(stats) { stats.associateBy { it.accountId } }
    Surface(
        shape = RoundedCornerShape(personalization.cornerStyle.cardDp.dp),
        color = personalization.configCardColor ?: cs.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        val contentColor = personalization.configCardTextColor
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Connection details",
                    style = MaterialTheme.typography.titleSmallEmphasized,
                    color = contentColor ?: cs.onSurface,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onRefreshPing, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = "Refresh ping",
                        modifier = Modifier.size(18.dp),
                        tint = contentColor ?: cs.onSurfaceVariant,
                    )
                }
            }
            DetailRow("Location", placeLabel, contentColor)
            DetailRow(
                "Ping",
                when (pingMs) {
                    -1 -> "measuring…"
                    -2 -> "unavailable"
                    else -> "$pingMs ms"
                },
                contentColor,
            )
            if (accounts.isEmpty()) {
                Text(
                    "No accounts yet — add some from the Accounts screen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor?.copy(alpha = 0.75f) ?: cs.onSurfaceVariant,
                )
            } else {
                accounts.forEach { account ->
                    val s = byId[account.id]
                    DetailRow(
                        label = account.email.ifBlank { "Account" },
                        value = when {
                            s == null || !account.isEnabled -> "idle"
                            s.isExhausted -> "quota exhausted"
                            !s.isConnected -> "dialing…"
                            else -> "↓ ${formatBytesPerSecond(s.speedDownBytesPerSec)} · ↑ ${formatBytesPerSecond(s.speedUpBytesPerSec)}"
                        },
                        contentColor = contentColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, contentColor: Color?) {
    val cs = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = contentColor?.copy(alpha = 0.75f) ?: cs.onSurfaceVariant,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            value,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor ?: cs.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ErrorCard(error: String, corner: Dp) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(corner),
        color = cs.errorContainer,
        contentColor = cs.onErrorContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            error,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
        )
    }
}

@Composable
private fun ServerRow(
    flag: String,
    name: String,
    detail: String,
    personalization: Personalization,
    corner: Dp,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(corner),
        color = personalization.serverCardColor ?: cs.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(cs.primary),
                contentAlignment = Alignment.Center,
            ) {
                if (flag.isNotBlank()) {
                    Text(flag, style = MaterialTheme.typography.titleLarge)
                } else {
                    Icon(Icons.Filled.Public, contentDescription = null, tint = cs.onPrimary)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMediumEmphasized, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = if (personalization.monospaceAddress) FontFamily.Monospace else FontFamily.Default,
                    ),
                    color = cs.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = cs.onSurfaceVariant)
        }
    }
}

@Composable
private fun AccountsRow(
    accounts: List<VpnAccount>,
    connectedCount: Int,
    state: ConnectionState,
    personalization: Personalization,
    corner: Dp,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val statusColors = LocalFoxyStatusColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(corner),
        color = personalization.serverActiveColor ?: cs.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(cs.tertiaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.AccountCircle, contentDescription = null, tint = cs.onTertiaryContainer)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Accounts", style = MaterialTheme.typography.titleMediumEmphasized)
                Text(
                    if (accounts.isEmpty()) "Tap to add your first account"
                    else "${accounts.count { it.isEnabled }} enabled · $connectedCount connected",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                accounts.take(8).forEach { account ->
                    val dot = when {
                        state == ConnectionState.CONNECTED && account.isEnabled -> statusColors.connected
                        !account.isEnabled -> cs.outlineVariant
                        else -> cs.outline
                    }
                    val animated by animateColorAsState(dot, tween(300), label = "acct-dot")
                    Box(Modifier.size(10.dp).clip(CircleShape).background(animated))
                }
            }
            Spacer(Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = cs.onSurfaceVariant)
        }
    }
}

/**
 * v1.3 lag fix: collects pool stats locally so the 2-second stats tick
 * recomposes ONLY this row, never the whole home screen.
 */
@Composable
private fun SpeedRow(live: Boolean, corner: Dp) {
    val cs = MaterialTheme.colorScheme
    val stats by FoxyVpnService.multiAccountStats.collectAsState()
    val down = stats.sumOf { it.speedDownBytesPerSec }
    val up = stats.sumOf { it.speedUpBytesPerSec }
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SpeedTile(
            icon = Icons.Filled.Download,
            label = "Download",
            value = if (live) formatBytesPerSecond(down) else "—",
            badge = cs.tertiaryContainer,
            onBadge = cs.onTertiaryContainer,
            corner = corner,
            modifier = Modifier.weight(1f),
        )
        SpeedTile(
            icon = Icons.Filled.Upload,
            label = "Upload",
            value = if (live) formatBytesPerSecond(up) else "—",
            badge = cs.primaryContainer,
            onBadge = cs.onPrimaryContainer,
            corner = corner,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SpeedTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    badge: Color,
    onBadge: Color,
    corner: Dp,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(corner), color = cs.surfaceContainerHigh, modifier = modifier) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(badge),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = onBadge, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.titleLargeEmphasized, maxLines = 1)
            }
        }
    }
}
