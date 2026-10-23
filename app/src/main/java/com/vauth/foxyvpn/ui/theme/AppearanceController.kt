package com.vauth.foxyvpn.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.vauth.foxyvpn.data.SettingsStore

/**
 * Reactive holder for every Interface/appearance setting. Each setter writes
 * through to [SettingsStore] (the persistence source of truth) and updates
 * Compose state so the whole UI recomposes instantly.
 */
class AppearanceController(private val settingsStore: SettingsStore) {

    var themeMode: ThemeMode by mutableStateOf(settingsStore.themeMode)
        private set
    var dynamicColor: Boolean by mutableStateOf(settingsStore.dynamicColor)
        private set
    var colorStyle: ColorStyle by mutableStateOf(settingsStore.colorStyle)
        private set
    var accentColorIndex: Int by mutableIntStateOf(settingsStore.accentColorIndex)
        private set
    var colorContrast: ColorContrast by mutableStateOf(settingsStore.colorContrast)
        private set
    var reduceMotion: Boolean by mutableStateOf(settingsStore.reduceMotion)
        private set
    var uiFontScale: UiFontScale by mutableStateOf(settingsStore.uiFontScale)
        private set
    var appLanguage: AppLanguage by mutableStateOf(settingsStore.appLanguage)
        private set
    var screenTransition: ScreenTransition by mutableStateOf(settingsStore.screenTransition)
        private set
    var navBarStyle: NavBarStyle by mutableStateOf(settingsStore.navBarStyle)
        private set
    var connectButtonStyle: ConnectButtonStyle by mutableStateOf(settingsStore.connectButtonStyle)
        private set
    var cardCornerStyle: CardCornerStyle by mutableStateOf(settingsStore.cardCornerStyle)
        private set
    var listDensity: ListDensity by mutableStateOf(settingsStore.listDensity)
        private set
    var themeProfileId: String? by mutableStateOf(settingsStore.themeProfileId)
        private set
    var appDesign: SettingsStore.AppDesign by mutableStateOf(settingsStore.appDesign)
        private set
    var personalization: Personalization by mutableStateOf(settingsStore.toPersonalization())
        private set
    var showServerPing: Boolean by mutableStateOf(settingsStore.showServerPing)
        private set
    var showServerUsage: Boolean by mutableStateOf(settingsStore.showServerUsage)
        private set
    var monospaceAddress: Boolean by mutableStateOf(settingsStore.monospaceAddress)
        private set
    var confirmRemove: Boolean by mutableStateOf(settingsStore.confirmRemove)
        private set
    var homeAfterSelect: Boolean by mutableStateOf(settingsStore.homeAfterSelect)
        private set
    var doubleColumnDisplay: Boolean by mutableStateOf(settingsStore.doubleColumnDisplay)
        private set

    fun setThemeMode(next: ThemeMode) {
        settingsStore.themeMode = next
        themeMode = next
    }

    /** AMOLED switch: maps onto ThemeMode.AMOLED like ZedSecure's amoledBlack. */
    fun setAmoled(on: Boolean) {
        setThemeMode(if (on) ThemeMode.AMOLED else ThemeMode.SYSTEM)
    }

    fun setDynamicColor(on: Boolean) {
        settingsStore.dynamicColor = on
        dynamicColor = on
    }

    fun setColorStyle(next: ColorStyle) {
        settingsStore.colorStyle = next
        colorStyle = next
        clearProfileSelection()
    }

    fun setAccentColorIndex(next: Int) {
        settingsStore.accentColorIndex = next
        accentColorIndex = next
        clearProfileSelection()
    }

    fun setColorContrast(next: ColorContrast) {
        settingsStore.colorContrast = next
        colorContrast = next
    }

    fun setReduceMotion(on: Boolean) {
        settingsStore.reduceMotion = on
        reduceMotion = on
    }

    fun setUiFontScale(next: UiFontScale) {
        settingsStore.uiFontScale = next
        uiFontScale = next
    }

    fun setAppLanguage(next: AppLanguage) {
        settingsStore.appLanguage = next
        appLanguage = next
    }

    fun setScreenTransition(next: ScreenTransition) {
        settingsStore.screenTransition = next
        screenTransition = next
    }

    fun setNavBarStyle(next: NavBarStyle) {
        settingsStore.navBarStyle = next
        navBarStyle = next
    }

    fun setConnectButtonStyle(next: ConnectButtonStyle) {
        settingsStore.connectButtonStyle = next
        connectButtonStyle = next
    }

    fun setCardCornerStyle(next: CardCornerStyle) {
        settingsStore.cardCornerStyle = next
        cardCornerStyle = next
        refreshPersonalization()
    }

    fun setListDensity(next: ListDensity) {
        settingsStore.listDensity = next
        listDensity = next
        refreshPersonalization()
    }

    fun setAppDesign(next: SettingsStore.AppDesign) {
        settingsStore.appDesign = next
        appDesign = next
    }

    fun setCustomColor(which: PersonalizeSlot, argb: Long?) {
        val value = argb ?: SettingsStore.UNSET_COLOR
        when (which) {
            PersonalizeSlot.Connect -> settingsStore.customConnectColor = value
            PersonalizeSlot.Connected -> settingsStore.customConnectedColor = value
            PersonalizeSlot.ServerCard -> settingsStore.customServerCardColor = value
            PersonalizeSlot.ServerActive -> settingsStore.customServerActiveColor = value
            PersonalizeSlot.ConfigCard -> settingsStore.customConfigCardColor = value
            PersonalizeSlot.ConfigCardText -> settingsStore.customConfigCardTextColor = value
        }
        refreshPersonalization()
        clearProfileSelection()
    }

    fun setShowServerPing(on: Boolean) {
        settingsStore.showServerPing = on
        showServerPing = on
        refreshPersonalization()
    }

    fun setShowServerUsage(on: Boolean) {
        settingsStore.showServerUsage = on
        showServerUsage = on
        refreshPersonalization()
    }

    fun setMonospaceAddress(on: Boolean) {
        settingsStore.monospaceAddress = on
        monospaceAddress = on
        refreshPersonalization()
    }

    fun setConfirmRemove(on: Boolean) {
        settingsStore.confirmRemove = on
        confirmRemove = on
    }

    fun setHomeAfterSelect(on: Boolean) {
        settingsStore.homeAfterSelect = on
        homeAfterSelect = on
    }

    fun setDoubleColumnDisplay(on: Boolean) {
        settingsStore.doubleColumnDisplay = on
        doubleColumnDisplay = on
    }

    fun applyThemeProfile(profile: ThemeProfile?) {
        if (profile == null) {
            settingsStore.resetPersonalization()
        } else {
            settingsStore.applyThemeProfile(profile)
        }
        themeProfileId = settingsStore.themeProfileId
        themeMode = settingsStore.themeMode
        accentColorIndex = settingsStore.accentColorIndex
        refreshPersonalization()
    }

    fun resetPersonalization() {
        settingsStore.resetPersonalization()
        themeProfileId = null
        refreshPersonalization()
    }

    fun hasPersonalization(): Boolean = settingsStore.hasPersonalization()

    private fun refreshPersonalization() {
        personalization = settingsStore.toPersonalization()
    }

    private fun clearProfileSelection() {
        if (themeProfileId != null) {
            settingsStore.themeProfileId = null
            themeProfileId = null
        }
    }
}

enum class PersonalizeSlot(val title: String) {
    Connect("Connect button"),
    Connected("Connected state"),
    ServerCard("Location card"),
    ServerActive("Active location card"),
    ConfigCard("Config card"),
    ConfigCardText("Config card text"),
}

@Composable
fun rememberAppearanceController(settingsStore: SettingsStore): AppearanceController =
    remember(settingsStore) { AppearanceController(settingsStore) }
