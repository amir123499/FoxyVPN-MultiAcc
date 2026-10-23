package com.vauth.foxyvpn.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// "Pixel" identity: a signal blue seed, tuned for the reskin.
val FoxSeed = Color(0xFF3B5BA9)

// ---- Light ----
val px_light_primary = Color(0xFF3B5BA9)
val px_light_onPrimary = Color(0xFFFFFFFF)
val px_light_primaryContainer = Color(0xFFD9E2FF)
val px_light_onPrimaryContainer = Color(0xFF001A40)
val px_light_inversePrimary = Color(0xFFAEC6FF)

val px_light_secondary = Color(0xFF575E71)
val px_light_onSecondary = Color(0xFFFFFFFF)
val px_light_secondaryContainer = Color(0xFFDBE2F9)
val px_light_onSecondaryContainer = Color(0xFF141B2C)

val px_light_tertiary = Color(0xFF6E5675)
val px_light_onTertiary = Color(0xFFFFFFFF)
val px_light_tertiaryContainer = Color(0xFFF7D8FF)
val px_light_onTertiaryContainer = Color(0xFF29132E)

val px_light_error = Color(0xFFBA1A1A)
val px_light_onError = Color(0xFFFFFFFF)
val px_light_errorContainer = Color(0xFFFFDAD6)
val px_light_onErrorContainer = Color(0xFF410002)

val px_light_background = Color(0xFFFDFBFF)
val px_light_onBackground = Color(0xFF1A1B1E)
val px_light_surface = Color(0xFFFDFBFF)
val px_light_onSurface = Color(0xFF1A1B1E)
val px_light_surfaceVariant = Color(0xFFE1E2EC)
val px_light_onSurfaceVariant = Color(0xFF44474E)
val px_light_surfaceTint = px_light_primary

val px_light_surfaceContainerLowest = Color(0xFFFFFFFF)
val px_light_surfaceContainerLow = Color(0xFFF5F3FA)
val px_light_surfaceContainer = Color(0xFFEFEDF4)
val px_light_surfaceContainerHigh = Color(0xFFE9E7EF)
val px_light_surfaceContainerHighest = Color(0xFFE3E1E9)
val px_light_surfaceBright = Color(0xFFFDFBFF)
val px_light_surfaceDim = Color(0xFFDBD9E0)

val px_light_outline = Color(0xFF74777F)
val px_light_outlineVariant = Color(0xFFC4C6D0)
val px_light_inverseSurface = Color(0xFF2F3033)
val px_light_inverseOnSurface = Color(0xFFF2F0F4)
val px_light_scrim = Color(0xFF000000)

// ---- Dark ----
val px_dark_primary = Color(0xFFAEC6FF)
val px_dark_onPrimary = Color(0xFF23325F)
val px_dark_primaryContainer = Color(0xFF3B5BA9)
val px_dark_onPrimaryContainer = Color(0xFFD9E2FF)
val px_dark_inversePrimary = Color(0xFF3B5BA9)

val px_dark_secondary = Color(0xFFBFC6DC)
val px_dark_onSecondary = Color(0xFF293041)
val px_dark_secondaryContainer = Color(0xFF3F4759)
val px_dark_onSecondaryContainer = Color(0xFFDBE2F9)

val px_dark_tertiary = Color(0xFFD8BDF1)
val px_dark_onTertiary = Color(0xFF3B2945)
val px_dark_tertiaryContainer = Color(0xFF533F5E)
val px_dark_onTertiaryContainer = Color(0xFFF7D8FF)

val px_dark_error = Color(0xFFFFB4AB)
val px_dark_onError = Color(0xFF690005)
val px_dark_errorContainer = Color(0xFF93000A)
val px_dark_onErrorContainer = Color(0xFFFFDAD6)

val px_dark_background = Color(0xFF131318)
val px_dark_onBackground = Color(0xFFE4E2E6)
val px_dark_surface = Color(0xFF131318)
val px_dark_onSurface = Color(0xFFE4E2E6)
val px_dark_surfaceVariant = Color(0xFF44474E)
val px_dark_onSurfaceVariant = Color(0xFFC4C6D0)
val px_dark_surfaceTint = px_dark_primary

val px_dark_surfaceContainerLowest = Color(0xFF0D0D12)
val px_dark_surfaceContainerLow = Color(0xFF1B1B21)
val px_dark_surfaceContainer = Color(0xFF1F1F25)
val px_dark_surfaceContainerHigh = Color(0xFF2A2A31)
val px_dark_surfaceContainerHighest = Color(0xFF35353D)
val px_dark_surfaceBright = Color(0xFF3A3A43)
val px_dark_surfaceDim = Color(0xFF131318)

val px_dark_outline = Color(0xFF8E9099)
val px_dark_outlineVariant = Color(0xFF44474E)
val px_dark_inverseSurface = Color(0xFFE4E2E6)
val px_dark_inverseOnSurface = Color(0xFF2F3033)
val px_dark_scrim = Color(0xFF000000)

// ---- AMOLED: pure-black surfaces, same hues as dark ----
val px_amoled_background = Color(0xFF000000)
val px_amoled_surface = Color(0xFF000000)
val px_amoled_surfaceContainerLowest = Color(0xFF000000)
val px_amoled_surfaceContainerLow = Color(0xFF0A0A0C)
val px_amoled_surfaceContainer = Color(0xFF101014)
val px_amoled_surfaceContainerHigh = Color(0xFF17171C)
val px_amoled_surfaceContainerHighest = Color(0xFF1E1E24)
val px_amoled_surfaceBright = Color(0xFF26262C)
val px_amoled_surfaceDim = Color(0xFF000000)
val px_amoled_surfaceVariant = Color(0xFF2A2A31)
val px_amoled_inverseSurface = Color(0xFFE4E2E6)
val px_amoled_inverseOnSurface = Color(0xFF131318)

data class FoxyStatusColors(
    val connected: Color,
    val connecting: Color,
    val disconnected: Color,
)

val LightStatusColors = FoxyStatusColors(
    connected = Color(0xFF146C2E),
    connecting = Color(0xFF8A5A00),
    disconnected = px_light_onSurfaceVariant,
)

val DarkStatusColors = FoxyStatusColors(
    connected = Color(0xFF78DC77),
    connecting = Color(0xFFFFC46B),
    disconnected = px_dark_onSurfaceVariant,
)

val AmoledStatusColors = FoxyStatusColors(
    connected = Color(0xFF78DC77),
    connecting = Color(0xFFFFC46B),
    disconnected = px_dark_onSurfaceVariant,
)

val LocalFoxyStatusColors = staticCompositionLocalOf { LightStatusColors }
