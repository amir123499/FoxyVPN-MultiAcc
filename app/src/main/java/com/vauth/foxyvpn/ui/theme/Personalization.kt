package com.vauth.foxyvpn.ui.theme

import androidx.compose.ui.graphics.Color
import com.vauth.foxyvpn.data.SettingsStore

/**
 * Per-surface personalization resolved from [SettingsStore]. Null = use the
 * color scheme default.
 */
data class Personalization(
    val connectColor: Color? = null,
    val connectedColor: Color? = null,
    val serverCardColor: Color? = null,
    val serverActiveColor: Color? = null,
    val configCardColor: Color? = null,
    val configCardTextColor: Color? = null,
    val cornerStyle: CardCornerStyle = CardCornerStyle.Rounded,
    val density: ListDensity = ListDensity.Comfortable,
    val showServerPing: Boolean = true,
    val showServerUsage: Boolean = true,
    val monospaceAddress: Boolean = false,
) {
    companion object {
        val Default = Personalization()
    }
}

private fun Long?.toColorOrNull(): Color? =
    if (this == null || this == SettingsStore.UNSET_COLOR) null else Color(this)

fun SettingsStore.toPersonalization(): Personalization = Personalization(
    connectColor = customConnectColor.toColorOrNull(),
    connectedColor = customConnectedColor.toColorOrNull(),
    serverCardColor = customServerCardColor.toColorOrNull(),
    serverActiveColor = customServerActiveColor.toColorOrNull(),
    configCardColor = customConfigCardColor.toColorOrNull(),
    configCardTextColor = customConfigCardTextColor.toColorOrNull(),
    cornerStyle = cardCornerStyle,
    density = listDensity,
    showServerPing = showServerPing,
    showServerUsage = showServerUsage,
    monospaceAddress = monospaceAddress,
)

fun SettingsStore.hasPersonalization(): Boolean =
    customConnectColor != SettingsStore.UNSET_COLOR ||
        customConnectedColor != SettingsStore.UNSET_COLOR ||
        customServerCardColor != SettingsStore.UNSET_COLOR ||
        customServerActiveColor != SettingsStore.UNSET_COLOR ||
        customConfigCardColor != SettingsStore.UNSET_COLOR ||
        customConfigCardTextColor != SettingsStore.UNSET_COLOR ||
        cardCornerStyle != CardCornerStyle.Rounded ||
        listDensity != ListDensity.Comfortable ||
        !showServerPing || !showServerUsage || monospaceAddress ||
        themeProfileId != null

fun SettingsStore.resetPersonalization() {
    customConnectColor = SettingsStore.UNSET_COLOR
    customConnectedColor = SettingsStore.UNSET_COLOR
    customServerCardColor = SettingsStore.UNSET_COLOR
    customServerActiveColor = SettingsStore.UNSET_COLOR
    customConfigCardColor = SettingsStore.UNSET_COLOR
    customConfigCardTextColor = SettingsStore.UNSET_COLOR
    themeProfileId = null
    cardCornerStyle = CardCornerStyle.Rounded
    listDensity = ListDensity.Comfortable
    showServerPing = true
    showServerUsage = true
    monospaceAddress = false
}

fun SettingsStore.applyThemeProfile(profile: ThemeProfile) {
    accentColorIndex = profile.accentIndex
    customConnectColor = profile.connect
    customConnectedColor = profile.connected
    customServerCardColor = profile.serverCard
    customServerActiveColor = profile.serverActive
    customConfigCardColor = profile.configCard
    customConfigCardTextColor = profile.configCardText
    themeProfileId = profile.id
    // AMOLED profile flag maps onto our AMOLED theme mode.
    themeMode = if (profile.amoled) ThemeMode.AMOLED else ThemeMode.SYSTEM
}
