package com.vauth.foxyvpn.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.vauth.foxyvpn.data.SettingsStore

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    AMOLED,
}

class ThemeController(private val settingsStore: SettingsStore) {

    var mode: ThemeMode by mutableStateOf(settingsStore.themeMode)
        private set

    fun resolveDark(systemInDarkTheme: Boolean): Boolean = when (mode) {
        ThemeMode.SYSTEM -> systemInDarkTheme
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.AMOLED -> true
    }

    fun isAmoled(): Boolean = mode == ThemeMode.AMOLED

    fun set(next: ThemeMode) {
        if (next == mode) return
        mode = next
        settingsStore.themeMode = next
    }

    fun toggle(systemInDarkTheme: Boolean) {
        // Cycles: light -> dark -> amoled -> light (or system -> dark first).
        set(
            when (mode) {
                ThemeMode.LIGHT -> ThemeMode.DARK
                ThemeMode.DARK -> ThemeMode.AMOLED
                ThemeMode.AMOLED -> ThemeMode.LIGHT
                ThemeMode.SYSTEM -> if (resolveDark(systemInDarkTheme)) ThemeMode.LIGHT else ThemeMode.DARK
            },
        )
    }
}

@Composable
fun rememberThemeController(settingsStore: SettingsStore): ThemeController =
    remember(settingsStore) { ThemeController(settingsStore) }
