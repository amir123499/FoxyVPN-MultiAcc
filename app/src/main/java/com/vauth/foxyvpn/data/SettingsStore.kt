package com.vauth.foxyvpn.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.vauth.foxyvpn.ui.theme.AppLanguage
import com.vauth.foxyvpn.ui.theme.CardCornerStyle
import com.vauth.foxyvpn.ui.theme.ColorContrast
import com.vauth.foxyvpn.ui.theme.ColorStyle
import com.vauth.foxyvpn.ui.theme.ConnectButtonStyle
import com.vauth.foxyvpn.ui.theme.ListDensity
import com.vauth.foxyvpn.ui.theme.NavBarStyle
import com.vauth.foxyvpn.ui.theme.ScreenTransition
import com.vauth.foxyvpn.ui.theme.ThemeMode
import com.vauth.foxyvpn.ui.theme.UiFontScale
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

    // ------------------------------------------------------------------
    // Interface / appearance settings (see AppearanceSettings.kt).
    // ------------------------------------------------------------------

    var colorStyle: ColorStyle
        get() = ColorStyle.entries.firstOrNull {
            it.name == prefs.getString(KEY_COLOR_STYLE, null)
        } ?: ColorStyle.ZedSecure
        set(value) { prefs.edit().putString(KEY_COLOR_STYLE, value.name).apply() }

    var dynamicColor: Boolean
        get() = prefs.getBoolean(KEY_DYNAMIC_COLOR, false)
        set(value) { prefs.edit().putBoolean(KEY_DYNAMIC_COLOR, value).apply() }

    var accentColorIndex: Int
        get() = prefs.getInt(KEY_ACCENT_COLOR_INDEX, DEFAULT_ACCENT_INDEX)
        set(value) { prefs.edit().putInt(KEY_ACCENT_COLOR_INDEX, value).apply() }

    var colorContrast: ColorContrast
        get() = ColorContrast.entries.firstOrNull {
            it.name == prefs.getString(KEY_COLOR_CONTRAST, null)
        } ?: ColorContrast.Standard
        set(value) { prefs.edit().putString(KEY_COLOR_CONTRAST, value.name).apply() }

    var reduceMotion: Boolean
        get() = prefs.getBoolean(KEY_REDUCE_MOTION, false)
        set(value) { prefs.edit().putBoolean(KEY_REDUCE_MOTION, value).apply() }

    var uiFontScale: UiFontScale
        get() = UiFontScale.entries.firstOrNull {
            it.name == prefs.getString(KEY_UI_FONT_SCALE, null)
        } ?: UiFontScale.Normal
        set(value) { prefs.edit().putString(KEY_UI_FONT_SCALE, value.name).apply() }

    var appLanguage: AppLanguage
        get() = AppLanguage.entries.firstOrNull {
            it.name == prefs.getString(KEY_APP_LANGUAGE, null)
        } ?: AppLanguage.System
        set(value) { prefs.edit().putString(KEY_APP_LANGUAGE, value.name).apply() }

    var screenTransition: ScreenTransition
        get() = ScreenTransition.entries.firstOrNull {
            it.name == prefs.getString(KEY_SCREEN_TRANSITION, null)
        } ?: ScreenTransition.Push
        set(value) { prefs.edit().putString(KEY_SCREEN_TRANSITION, value.name).apply() }

    var navBarStyle: NavBarStyle
        get() = NavBarStyle.entries.firstOrNull {
            it.name == prefs.getString(KEY_NAV_BAR_STYLE, null)
        } ?: NavBarStyle.ExpressivePill
        set(value) { prefs.edit().putString(KEY_NAV_BAR_STYLE, value.name).apply() }

    var connectButtonStyle: ConnectButtonStyle
        get() = ConnectButtonStyle.entries.firstOrNull {
            it.name == prefs.getString(KEY_CONNECT_BUTTON_STYLE, null)
        } ?: ConnectButtonStyle.Hero
        set(value) { prefs.edit().putString(KEY_CONNECT_BUTTON_STYLE, value.name).apply() }

    var cardCornerStyle: CardCornerStyle
        get() = CardCornerStyle.entries.firstOrNull {
            it.name == prefs.getString(KEY_CARD_CORNER_STYLE, null)
        } ?: CardCornerStyle.Rounded
        set(value) { prefs.edit().putString(KEY_CARD_CORNER_STYLE, value.name).apply() }

    var listDensity: ListDensity
        get() = ListDensity.entries.firstOrNull {
            it.name == prefs.getString(KEY_LIST_DENSITY, null)
        } ?: ListDensity.Comfortable
        set(value) { prefs.edit().putString(KEY_LIST_DENSITY, value.name).apply() }

    var themeProfileId: String?
        get() = prefs.getString(KEY_THEME_PROFILE_ID, null)
        set(value) { prefs.edit().putString(KEY_THEME_PROFILE_ID, value).apply() }

    var customConnectColor: Long
        get() = prefs.getLong(KEY_CUSTOM_CONNECT_COLOR, UNSET_COLOR)
        set(value) { prefs.edit().putLong(KEY_CUSTOM_CONNECT_COLOR, value).apply() }

    var customConnectedColor: Long
        get() = prefs.getLong(KEY_CUSTOM_CONNECTED_COLOR, UNSET_COLOR)
        set(value) { prefs.edit().putLong(KEY_CUSTOM_CONNECTED_COLOR, value).apply() }

    var customServerCardColor: Long
        get() = prefs.getLong(KEY_CUSTOM_SERVER_CARD_COLOR, UNSET_COLOR)
        set(value) { prefs.edit().putLong(KEY_CUSTOM_SERVER_CARD_COLOR, value).apply() }

    var customServerActiveColor: Long
        get() = prefs.getLong(KEY_CUSTOM_SERVER_ACTIVE_COLOR, UNSET_COLOR)
        set(value) { prefs.edit().putLong(KEY_CUSTOM_SERVER_ACTIVE_COLOR, value).apply() }

    var customConfigCardColor: Long
        get() = prefs.getLong(KEY_CUSTOM_CONFIG_CARD_COLOR, UNSET_COLOR)
        set(value) { prefs.edit().putLong(KEY_CUSTOM_CONFIG_CARD_COLOR, value).apply() }

    var customConfigCardTextColor: Long
        get() = prefs.getLong(KEY_CUSTOM_CONFIG_CARD_TEXT_COLOR, UNSET_COLOR)
        set(value) { prefs.edit().putLong(KEY_CUSTOM_CONFIG_CARD_TEXT_COLOR, value).apply() }

    var showServerPing: Boolean
        get() = prefs.getBoolean(KEY_SHOW_SERVER_PING, true)
        set(value) { prefs.edit().putBoolean(KEY_SHOW_SERVER_PING, value).apply() }

    var showServerUsage: Boolean
        get() = prefs.getBoolean(KEY_SHOW_SERVER_USAGE, true)
        set(value) { prefs.edit().putBoolean(KEY_SHOW_SERVER_USAGE, value).apply() }

    var monospaceAddress: Boolean
        get() = prefs.getBoolean(KEY_MONOSPACE_ADDRESS, false)
        set(value) { prefs.edit().putBoolean(KEY_MONOSPACE_ADDRESS, value).apply() }

    var confirmRemove: Boolean
        get() = prefs.getBoolean(KEY_CONFIRM_REMOVE, true)
        set(value) { prefs.edit().putBoolean(KEY_CONFIRM_REMOVE, value).apply() }

    var homeAfterSelect: Boolean
        get() = prefs.getBoolean(KEY_HOME_AFTER_SELECT, false)
        set(value) { prefs.edit().putBoolean(KEY_HOME_AFTER_SELECT, value).apply() }

    var doubleColumnDisplay: Boolean
        get() = prefs.getBoolean(KEY_DOUBLE_COLUMN_DISPLAY, false)
        set(value) { prefs.edit().putBoolean(KEY_DOUBLE_COLUMN_DISPLAY, value).apply() }

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
        private const val KEY_COLOR_STYLE = "color_style"
        private const val KEY_DYNAMIC_COLOR = "dynamic_color"
        private const val KEY_ACCENT_COLOR_INDEX = "accent_color_index"
        /** Signal blue (see AccentPresets). Existing stored choices are unaffected. */
        const val DEFAULT_ACCENT_INDEX = 10
        private const val KEY_COLOR_CONTRAST = "color_contrast"
        private const val KEY_REDUCE_MOTION = "reduce_motion"
        private const val KEY_UI_FONT_SCALE = "ui_font_scale"
        private const val KEY_APP_LANGUAGE = "app_language"
        private const val KEY_SCREEN_TRANSITION = "screen_transition"
        private const val KEY_NAV_BAR_STYLE = "nav_bar_style"
        private const val KEY_CONNECT_BUTTON_STYLE = "connect_button_style"
        private const val KEY_CARD_CORNER_STYLE = "card_corner_style"
        private const val KEY_LIST_DENSITY = "list_density"
        private const val KEY_THEME_PROFILE_ID = "theme_profile_id"
        private const val KEY_CUSTOM_CONNECT_COLOR = "custom_connect_color"
        private const val KEY_CUSTOM_CONNECTED_COLOR = "custom_connected_color"
        private const val KEY_CUSTOM_SERVER_CARD_COLOR = "custom_server_card_color"
        private const val KEY_CUSTOM_SERVER_ACTIVE_COLOR = "custom_server_active_color"
        private const val KEY_CUSTOM_CONFIG_CARD_COLOR = "custom_config_card_color"
        private const val KEY_CUSTOM_CONFIG_CARD_TEXT_COLOR = "custom_config_card_text_color"
        private const val KEY_SHOW_SERVER_PING = "show_server_ping"
        private const val KEY_SHOW_SERVER_USAGE = "show_server_usage"
        private const val KEY_MONOSPACE_ADDRESS = "monospace_address"
        private const val KEY_CONFIRM_REMOVE = "confirm_remove"
        private const val KEY_HOME_AFTER_SELECT = "home_after_select"
        private const val KEY_DOUBLE_COLUMN_DISPLAY = "double_column_display"
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

        /** Sentinel for "no custom color chosen" in the personalize color settings. */
        const val UNSET_COLOR: Long = Long.MIN_VALUE

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
