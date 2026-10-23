package com.vauth.foxyvpn.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.vauth.foxyvpn.ui.theme.ThemeMode
import com.vauth.foxyvpn.vpn.FoxyVpnService
import com.vauth.foxyvpn.widget.QuickConnectWidgetProvider

class SettingsStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("foxyvpn_settings", Context.MODE_PRIVATE)

    private val credentialsPrefs by lazy {
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            appContext,
            "foxyvpn_settings_credentials",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    var themeMode: ThemeMode
        get() = ThemeMode.entries.firstOrNull {
            it.name == prefs.getString(KEY_THEME_MODE, null)
        } ?: ThemeMode.SYSTEM
        set(value) {
            prefs.edit().putString(KEY_THEME_MODE, value.name).apply()
            QuickConnectWidgetProvider.updateAll(appContext, FoxyVpnService.state.value)
        }

    var appDesign: AppDesign
        get() = AppDesign.entries.firstOrNull {
            it.name == prefs.getString(KEY_APP_DESIGN, null)
        } ?: AppDesign.PIXEL
        set(value) { prefs.edit().putString(KEY_APP_DESIGN, value.name).apply() }

    var exitCheckEnabled: Boolean
        get() = prefs.getBoolean(KEY_EXIT_CHECK, true)
        set(value) { prefs.edit().putBoolean(KEY_EXIT_CHECK, value).apply() }

    var socksBindAddress: String
        get() = prefs.getString(KEY_SOCKS_BIND_ADDRESS, DEFAULT_SOCKS_BIND_ADDRESS) ?: DEFAULT_SOCKS_BIND_ADDRESS
        set(value) {
            prefs.edit().putString(KEY_SOCKS_BIND_ADDRESS, value.ifBlank { DEFAULT_SOCKS_BIND_ADDRESS }).apply()
        }

    var socksPort: Int
        get() = prefs.getInt(KEY_SOCKS_PORT, DEFAULT_SOCKS_PORT)
        set(value) { prefs.edit().putInt(KEY_SOCKS_PORT, value.coerceIn(1, 65_535)).apply() }

    var httpProxyEnabled: Boolean
        get() = prefs.getBoolean(KEY_HTTP_PROXY_ENABLED, false)
        set(value) { prefs.edit().putBoolean(KEY_HTTP_PROXY_ENABLED, value).apply() }

    var httpProxyPort: Int
        get() = prefs.getInt(KEY_HTTP_PROXY_PORT, DEFAULT_HTTP_PROXY_PORT)
        set(value) { prefs.edit().putInt(KEY_HTTP_PROXY_PORT, value.coerceIn(1, 65_535)).apply() }

    var dnsProvider: DnsProvider
        get() = DnsProvider.entries.firstOrNull {
            it.name == prefs.getString(KEY_DNS_PROVIDER, null)
        } ?: DnsProvider.SYSTEM_DEFAULT
        set(value) { prefs.edit().putString(KEY_DNS_PROVIDER, value.name).apply() }

    var customDohUrl: String
        get() = prefs.getString(KEY_CUSTOM_DOH_URL, "") ?: ""
        set(value) {
            val trimmed = value.trim()
            if (trimmed.isNotEmpty() && !isValidDohUrl(trimmed)) return
            prefs.edit().putString(KEY_CUSTOM_DOH_URL, trimmed).apply()
        }

    val effectiveDohUrl: String?
        get() = when (dnsProvider) {
            DnsProvider.SYSTEM_DEFAULT -> null
            DnsProvider.CUSTOM -> customDohUrl.takeIf { isValidDohUrl(it) }
            else -> dnsProvider.dohUrl
        }

    var bootstrapDnsProvider: BootstrapDnsProvider
        get() = BootstrapDnsProvider.entries.firstOrNull {
            it.name == prefs.getString(KEY_BOOTSTRAP_DNS_PROVIDER, null)
        } ?: BootstrapDnsProvider.AUTOMATIC
        set(value) { prefs.edit().putString(KEY_BOOTSTRAP_DNS_PROVIDER, value.name).apply() }

    var customBootstrapIp: String
        get() = prefs.getString(KEY_CUSTOM_BOOTSTRAP_IP, "") ?: ""
        set(value) {
            val trimmed = value.trim()
            if (trimmed.isNotEmpty() && !isValidIpAddress(trimmed)) return
            prefs.edit().putString(KEY_CUSTOM_BOOTSTRAP_IP, trimmed).apply()
        }

    val effectiveBootstrapAddresses: List<String>
        get() = when (bootstrapDnsProvider) {
            BootstrapDnsProvider.CUSTOM -> {
                val ip = customBootstrapIp.trim()
                if (isValidIpAddress(ip)) listOf(ip) else BootstrapDnsProvider.AUTOMATIC.addresses
            }
            BootstrapDnsProvider.SYSTEM -> emptyList()
            else -> bootstrapDnsProvider.addresses
        }

    var proxyOnlyMode: Boolean
        get() = prefs.getBoolean(KEY_PROXY_ONLY_MODE, false)
        set(value) { prefs.edit().putBoolean(KEY_PROXY_ONLY_MODE, value).apply() }

    var customEdgeAddress: String
        get() = prefs.getString(KEY_CUSTOM_EDGE_ADDRESS, "") ?: ""
        set(value) {
            val normalized = value.trim().lowercase()
            if (normalized.isNotEmpty() && !isValidEdgeHost(normalized)) return
            prefs.edit().putString(KEY_CUSTOM_EDGE_ADDRESS, normalized).apply()
        }

    val effectiveCustomEdgeAddress: String?
        get() = customEdgeAddress.takeIf { isValidEdgeHost(it) }

    var upstreamProxyEnabled: Boolean
        get() = prefs.getBoolean(KEY_UPSTREAM_PROXY_ENABLED, false)
        set(value) { prefs.edit().putBoolean(KEY_UPSTREAM_PROXY_ENABLED, value).apply() }

    var upstreamProxyType: UpstreamProxyType
        get() = UpstreamProxyType.entries.firstOrNull {
            it.name == prefs.getString(KEY_UPSTREAM_PROXY_TYPE, null)
        } ?: UpstreamProxyType.SOCKS5
        set(value) { prefs.edit().putString(KEY_UPSTREAM_PROXY_TYPE, value.name).apply() }

    var upstreamProxyHost: String
        get() = prefs.getString(KEY_UPSTREAM_PROXY_HOST, "") ?: ""
        set(value) { prefs.edit().putString(KEY_UPSTREAM_PROXY_HOST, value.trim()).apply() }

    var upstreamProxyPort: Int
        get() = prefs.getInt(KEY_UPSTREAM_PROXY_PORT, DEFAULT_UPSTREAM_PROXY_PORT)
        set(value) { prefs.edit().putInt(KEY_UPSTREAM_PROXY_PORT, value.coerceIn(1, 65_535)).apply() }

    var upstreamProxyUsername: String
        get() = credentialsPrefs.getString(KEY_UPSTREAM_PROXY_USERNAME, "") ?: ""
        set(value) { credentialsPrefs.edit().putString(KEY_UPSTREAM_PROXY_USERNAME, value).apply() }

    var upstreamProxyPassword: String
        get() = credentialsPrefs.getString(KEY_UPSTREAM_PROXY_PASSWORD, "") ?: ""
        set(value) { credentialsPrefs.edit().putString(KEY_UPSTREAM_PROXY_PASSWORD, value).apply() }

    var excludedApps: Set<String>
        get() = prefs.getStringSet(KEY_EXCLUDED_APPS, emptySet())?.toSet() ?: emptySet()
        set(value) {

            prefs.edit().putStringSet(KEY_EXCLUDED_APPS, HashSet(value)).apply()
        }

    enum class UpstreamProxyType { SOCKS5, HTTP }

    enum class AppDesign(val label: String) {
        PIXEL(label = "Pixel"),
        PORTAL(label = "Portal"),
        OLD(label = "Classic"),
    }

    enum class DnsProvider(
        val label: String,
        val dohUrl: String?,
    ) {
        SYSTEM_DEFAULT(
            label = "Default",
            dohUrl = null,
        ),
        CLOUDFLARE(
            label = "Cloudflare",
            dohUrl = "https://1.1.1.1/dns-query",
        ),
        GOOGLE(
            label = "Google",
            dohUrl = "https://8.8.8.8/dns-query",
        ),
        QUAD9(
            label = "Quad9",
            dohUrl = "https://9.9.9.9/dns-query",
        ),
        ADGUARD(
            label = "AdGuard",
            dohUrl = "https://dns.adguard-dns.com/dns-query",
        ),
        CUSTOM(
            label = "Custom DoH",
            dohUrl = null,
        ),
        ;
    }

    enum class BootstrapDnsProvider(
        val label: String,
        val description: String,
        val addresses: List<String>,
    ) {
        AUTOMATIC(
            label = "Automatic",
            description = "Races all default DoH endpoints",
            addresses = listOf("9.9.9.9", "1.1.1.1", "8.8.8.8"),
        ),
        QUAD9(
            label = "Quad9",
            description = "9.9.9.9, 149.112.112.112",
            addresses = listOf("9.9.9.9", "149.112.112.112"),
        ),
        CLOUDFLARE(
            label = "Cloudflare",
            description = "1.1.1.1, 1.0.0.1",
            addresses = listOf("1.1.1.1", "1.0.0.1"),
        ),
        GOOGLE(
            label = "Google",
            description = "8.8.8.8, 8.8.4.4",
            addresses = listOf("8.8.8.8", "8.8.4.4"),
        ),
        SYSTEM(
            label = "System DNS",
            description = "Local network DNS",
            addresses = emptyList(),
        ),
        CUSTOM(
            label = "Custom IP",
            description = "Specify a custom DoH IP resolver address",
            addresses = emptyList(),
        ),
        ;
    }

    companion object {
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_APP_DESIGN = "app_design"
        private const val KEY_EXIT_CHECK = "exit_check_enabled"
        private const val KEY_SOCKS_BIND_ADDRESS = "socks_bind_address"
        private const val KEY_SOCKS_PORT = "socks_port"
        private const val KEY_HTTP_PROXY_ENABLED = "http_proxy_enabled"
        private const val KEY_HTTP_PROXY_PORT = "http_proxy_port"
        private const val KEY_DNS_PROVIDER = "dns_provider"
        private const val KEY_CUSTOM_DOH_URL = "custom_doh_url"
        private const val KEY_BOOTSTRAP_DNS_PROVIDER = "bootstrap_dns_provider"
        private const val KEY_CUSTOM_BOOTSTRAP_IP = "custom_bootstrap_ip"
        private const val KEY_PROXY_ONLY_MODE = "proxy_only_mode"
        private const val KEY_CUSTOM_EDGE_ADDRESS = "custom_edge_address"
        private const val KEY_UPSTREAM_PROXY_ENABLED = "upstream_proxy_enabled"
        private const val KEY_UPSTREAM_PROXY_TYPE = "upstream_proxy_type"
        private const val KEY_UPSTREAM_PROXY_HOST = "upstream_proxy_host"
        private const val KEY_UPSTREAM_PROXY_PORT = "upstream_proxy_port"
        private const val KEY_UPSTREAM_PROXY_USERNAME = "upstream_proxy_username"
        private const val KEY_UPSTREAM_PROXY_PASSWORD = "upstream_proxy_password"
        private const val KEY_EXCLUDED_APPS = "excluded_apps"
        const val DEFAULT_SOCKS_BIND_ADDRESS = "127.0.0.1"
        const val DEFAULT_SOCKS_PORT = 1080
        const val DEFAULT_HTTP_PROXY_PORT = 8080
        const val DEFAULT_UPSTREAM_PROXY_PORT = 1080

        val SOCKS_BIND_ADDRESS_PRESETS = listOf(
            "127.0.0.1" to "Loopback only (127.0.0.1)",
            "0.0.0.0" to "All interfaces (0.0.0.0)",
        )

        fun isValidDohUrl(value: String): Boolean {
            val trimmed = value.trim()
            if (!trimmed.startsWith("https://", ignoreCase = true)) return false
            return runCatching {
                val uri = java.net.URI.create(trimmed)
                !uri.host.isNullOrBlank()
            }.getOrDefault(false)
        }

        fun isValidIpAddress(value: String): Boolean {
            return if (android.os.Build.VERSION.SDK_INT >= 29) {
                android.net.InetAddresses.isNumericAddress(value)
            } else {
                @Suppress("DEPRECATION")
                android.util.Patterns.IP_ADDRESS.matcher(value).matches()
            }
        }

        fun isValidHostname(value: String): Boolean {
            val host = value.trim().removeSuffix(".")
            if (host.isEmpty() || host.length > 253) return false

            if (host.contains(':')) return false
            if (host.all { it.isDigit() || it == '.' }) return false
            val labels = host.split('.')
            if (labels.size < 2) return false
            return labels.all { label ->
                label.isNotEmpty() &&
                    label.length <= 63 &&
                    !label.startsWith('-') &&
                    !label.endsWith('-') &&
                    label.all { char ->
                        char == '-' || (char.code < 128 && (char.isLetterOrDigit()))
                    }
            }
        }

        fun isValidEdgeHost(value: String): Boolean {
            val host = value.trim().removeSuffix(".")
            return isValidHostname(host) || isValidIpAddress(host)
        }
    }
}
