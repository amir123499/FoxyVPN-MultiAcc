package com.vauth.foxyvpn.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.core.graphics.ColorUtils

/**
 * Interface/appearance options ported from ZedSecure's Ui settings page.
 * Design language studied from ZedSecure; all code below is original.
 */

enum class ColorStyle(val label: String) {
    ZedSecure("ZedSecure"),
    TonalSpot("Tonal Spot"),
    Vibrant("Vibrant"),
    Expressive("Expressive"),
    Rainbow("Rainbow"),
    FruitSalad("Fruit Salad"),
    Fidelity("Fidelity"),
    Neutral("Neutral"),
    Monochrome("Monochrome"),
}

enum class ColorContrast(val label: String, val level: Double) {
    Standard("Standard", 0.0),
    Medium("Medium", 0.5),
    High("High", 1.0),
}

enum class AppLanguage(val label: String, val tag: String?) {
    System("System", null),
    English("English", "en"),
    Persian("فارسی", "fa"),
}

enum class UiFontScale(val label: String, val scale: Float) {
    Smallest("Smallest", 0.80f),
    Smaller("Smaller", 0.90f),
    Normal("Normal", 1.00f),
    Larger("Larger", 1.12f),
    Largest("Largest", 1.25f),
}

enum class ScreenTransition(val label: String) {
    Fade("Fade"),
    Slide("Slide"),
    Push("Push"),
    Depth("Depth"),
    Elastic("Elastic"),
    Curtain("Curtain"),
    Flip("Flip"),
}

enum class NavBarStyle(val label: String) {
    FloatingPill("Floating pill"),
    CompactDock("Compact dock"),
    ExpressivePill("Expressive pill"),
    FullBar("Full bar"),
    Underline("Underline"),
    Minimal("Minimal"),
}

enum class ConnectButtonStyle(val label: String) {
    Pill("Pill"),
    Hero("Hero"),
    Ring("Ring"),
    Bar("Bar"),
    Icon("Icon"),
    Switch("Switch"),
}

enum class CardCornerStyle(val label: String, val cardDp: Int) {
    Squared("Squared", 10),
    Rounded("Rounded", 28),
    Pill("Pill", 48),
}

enum class ListDensity(val label: String) {
    Comfortable("Comfortable"),
    Compact("Compact"),
}

data class AccentPreset(val name: String, val light: Color, val dark: Color)

val AccentPresets: List<AccentPreset> = listOf(
    AccentPreset("Iris", Color(0xFF5646D6), Color(0xFFC7BFFF)),
    AccentPreset("Ocean", Color(0xFF1E63D0), Color(0xFFAAC7FF)),
    AccentPreset("Teal", Color(0xFF00897E), Color(0xFF52DDCF)),
    AccentPreset("Forest", Color(0xFF2E7D32), Color(0xFF9BD89B)),
    AccentPreset("Amber", Color(0xFFB4690E), Color(0xFFFFB870)),
    AccentPreset("Rose", Color(0xFFC4326B), Color(0xFFFFB0C8)),
    AccentPreset("Crimson", Color(0xFFC0362C), Color(0xFFFFB4A9)),
    AccentPreset("Cyan", Color(0xFF00838F), Color(0xFF4DD0E1)),
    AccentPreset("Grape", Color(0xFF8E24AA), Color(0xFFE9B3FF)),
    AccentPreset("Slate", Color(0xFF4A5B7A), Color(0xFFB6C6EA)),
)

fun accentSeed(index: Int, dark: Boolean): Color {
    val preset = AccentPresets.getOrNull(index) ?: AccentPresets[0]
    return if (dark) preset.dark else preset.light
}

private fun hslColor(h: Float, s: Float, l: Float): Color {
    val hue = ((h % 360f) + 360f) % 360f
    val packed = ColorUtils.HSLToColor(floatArrayOf(hue, s.coerceIn(0f, 1f), l.coerceIn(0f, 1f)))
    return Color(packed)
}

private fun seedHsl(seed: Color): FloatArray {
    val out = FloatArray(3)
    ColorUtils.RGBToHSL(
        (seed.red * 255f).toInt(),
        (seed.green * 255f).toInt(),
        (seed.blue * 255f).toInt(),
        out,
    )
    return out
}

private fun Color.readableOn(): Color = if (luminance() > 0.5f) Color(0xFF101418) else Color.White

/**
 * Builds a full Material3 [ColorScheme] from a seed color, in the spirit of
 * ZedSecure's material-kolor based generator, using only androidx.core HSL
 * math (no extra dependency). Surfaces carry a subtle tint of the seed hue
 * so the app background feels alive but tasteful.
 */
fun seedColorScheme(
    dark: Boolean,
    seed: Color,
    style: ColorStyle,
    contrast: ColorContrast,
): ColorScheme {
    val hsl = seedHsl(seed)
    val hue = hsl[0]
    val satMul = when (style) {
        ColorStyle.ZedSecure, ColorStyle.TonalSpot -> 0.9f
        ColorStyle.Vibrant -> 1.25f
        ColorStyle.Expressive -> 1.1f
        ColorStyle.Rainbow -> 1.2f
        ColorStyle.FruitSalad -> 1.15f
        ColorStyle.Fidelity -> 1.0f
        ColorStyle.Neutral -> 0.55f
        ColorStyle.Monochrome -> 0.0f
    }
    val hueVar = when (style) {
        ColorStyle.Rainbow -> 90f
        ColorStyle.FruitSalad -> 60f
        ColorStyle.Expressive -> 45f
        else -> 32f
    }
    val s = (hsl[1] * satMul).coerceIn(0f, 1f)

    // Contrast pushes primaries toward vivid and surfaces toward pure.
    val contrastBoost = when (contrast) {
        ColorContrast.Standard -> 0f
        ColorContrast.Medium -> 0.06f
        ColorContrast.High -> 0.12f
    }

    val primary: Color
    val onPrimary: Color
    val primaryContainer: Color
    val onPrimaryContainer: Color
    val secondary: Color
    val onSecondary: Color
    val secondaryContainer: Color
    val onSecondaryContainer: Color
    val tertiary: Color
    val onTertiary: Color
    val tertiaryContainer: Color
    val onTertiaryContainer: Color
    val background: Color
    val onBackground: Color
    val surface: Color
    val onSurface: Color
    val surfaceVariant: Color
    val onSurfaceVariant: Color
    val surfaceTint: Color
    val surfaceBright: Color
    val surfaceDim: Color
    val surfaceContainer: Color
    val surfaceContainerHigh: Color
    val surfaceContainerHighest: Color
    val surfaceContainerLow: Color
    val surfaceContainerLowest: Color
    val outline: Color
    val outlineVariant: Color
    val inverseSurface: Color
    val inverseOnSurface: Color
    val error: Color
    val onError: Color
    val errorContainer: Color
    val onErrorContainer: Color

    if (!dark) {
        primary = hslColor(hue, (s + contrastBoost).coerceIn(0f, 1f), 0.42f)
        onPrimary = primary.readableOn()
        primaryContainer = hslColor(hue, s * 0.45f, 0.90f)
        onPrimaryContainer = hslColor(hue, s * 0.7f, 0.16f)
        secondary = hslColor(hue + hueVar, s * 0.8f, 0.44f)
        onSecondary = secondary.readableOn()
        secondaryContainer = hslColor(hue + hueVar, s * 0.4f, 0.90f)
        onSecondaryContainer = hslColor(hue + hueVar, s * 0.7f, 0.18f)
        tertiary = hslColor(hue - hueVar, s * 0.85f, 0.44f)
        onTertiary = tertiary.readableOn()
        tertiaryContainer = hslColor(hue - hueVar, s * 0.4f, 0.90f)
        onTertiaryContainer = hslColor(hue - hueVar, s * 0.7f, 0.18f)
        // Cheerful subtle tint: background carries ~6% of the seed hue.
        background = hslColor(hue, s * 0.10f, if (contrast == ColorContrast.High) 1f else 0.97f)
        onBackground = if (contrast == ColorContrast.High) Color.Black else Color(0xFF191C22)
        surface = hslColor(hue, s * 0.12f, if (contrast == ColorContrast.High) 1f else 0.96f)
        onSurface = if (contrast == ColorContrast.High) Color.Black else Color(0xFF191C22)
        surfaceVariant = hslColor(hue, s * 0.16f, 0.90f)
        onSurfaceVariant = hslColor(hue, s * 0.25f, 0.32f)
        surfaceTint = primary
        surfaceBright = hslColor(hue, s * 0.10f, 0.98f)
        surfaceDim = hslColor(hue, s * 0.14f, 0.88f)
        surfaceContainerLowest = hslColor(hue, s * 0.08f, 1f)
        surfaceContainerLow = hslColor(hue, s * 0.10f, 0.96f)
        surfaceContainer = hslColor(hue, s * 0.12f, 0.93f)
        surfaceContainerHigh = hslColor(hue, s * 0.14f, 0.90f)
        surfaceContainerHighest = hslColor(hue, s * 0.16f, 0.87f)
        outline = hslColor(hue, s * 0.20f, 0.55f)
        outlineVariant = hslColor(hue, s * 0.14f, 0.80f)
        inverseSurface = Color(0xFF2E3138)
        inverseOnSurface = Color(0xFFF0F1F5)
        error = hslColor(4f, 0.72f, 0.44f)
        onError = Color.White
        errorContainer = hslColor(4f, 0.6f, 0.90f)
        onErrorContainer = hslColor(4f, 0.7f, 0.18f)
    } else {
        primary = hslColor(hue, (s + contrastBoost).coerceIn(0f, 1f), 0.82f)
        onPrimary = primary.readableOn()
        primaryContainer = hslColor(hue, s * 0.7f, 0.30f)
        onPrimaryContainer = hslColor(hue, s * 0.5f, 0.90f)
        secondary = hslColor(hue + hueVar, s * 0.7f, 0.78f)
        onSecondary = secondary.readableOn()
        secondaryContainer = hslColor(hue + hueVar, s * 0.6f, 0.30f)
        onSecondaryContainer = hslColor(hue + hueVar, s * 0.5f, 0.90f)
        tertiary = hslColor(hue - hueVar, s * 0.7f, 0.78f)
        onTertiary = tertiary.readableOn()
        tertiaryContainer = hslColor(hue - hueVar, s * 0.6f, 0.30f)
        onTertiaryContainer = hslColor(hue - hueVar, s * 0.5f, 0.90f)
        background = hslColor(hue, s * 0.16f, if (contrast == ColorContrast.High) 0f else 0.07f)
        onBackground = if (contrast == ColorContrast.High) Color.White else Color(0xFFE3E5EA)
        surface = hslColor(hue, s * 0.16f, if (contrast == ColorContrast.High) 0f else 0.07f)
        onSurface = if (contrast == ColorContrast.High) Color.White else Color(0xFFE3E5EA)
        surfaceVariant = hslColor(hue, s * 0.20f, 0.20f)
        onSurfaceVariant = hslColor(hue, s * 0.25f, 0.72f)
        surfaceTint = primary
        surfaceBright = hslColor(hue, s * 0.16f, 0.16f)
        surfaceDim = hslColor(hue, s * 0.18f, 0.05f)
        surfaceContainerLowest = hslColor(hue, s * 0.18f, 0.04f)
        surfaceContainerLow = hslColor(hue, s * 0.16f, 0.09f)
        surfaceContainer = hslColor(hue, s * 0.16f, 0.12f)
        surfaceContainerHigh = hslColor(hue, s * 0.16f, 0.16f)
        surfaceContainerHighest = hslColor(hue, s * 0.18f, 0.21f)
        outline = hslColor(hue, s * 0.20f, 0.45f)
        outlineVariant = hslColor(hue, s * 0.22f, 0.26f)
        inverseSurface = Color(0xFFE3E5EA)
        inverseOnSurface = Color(0xFF2E3138)
        error = hslColor(4f, 0.72f, 0.72f)
        onError = Color(0xFF40090B)
        errorContainer = hslColor(4f, 0.6f, 0.30f)
        onErrorContainer = hslColor(4f, 0.6f, 0.90f)
    }

    val scheme = if (!dark) {
        lightColorScheme(
            primary = primary, onPrimary = onPrimary,
            primaryContainer = primaryContainer, onPrimaryContainer = onPrimaryContainer,
            inversePrimary = hslColor(hue, s, 0.80f),
            secondary = secondary, onSecondary = onSecondary,
            secondaryContainer = secondaryContainer, onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary, onTertiary = onTertiary,
            tertiaryContainer = tertiaryContainer, onTertiaryContainer = onTertiaryContainer,
            background = background, onBackground = onBackground,
            surface = surface, onSurface = onSurface,
            surfaceVariant = surfaceVariant, onSurfaceVariant = onSurfaceVariant,
            surfaceTint = surfaceTint,
            surfaceBright = surfaceBright, surfaceDim = surfaceDim,
            surfaceContainer = surfaceContainer, surfaceContainerHigh = surfaceContainerHigh,
            surfaceContainerHighest = surfaceContainerHighest,
            surfaceContainerLow = surfaceContainerLow, surfaceContainerLowest = surfaceContainerLowest,
            outline = outline, outlineVariant = outlineVariant,
            inverseSurface = inverseSurface, inverseOnSurface = inverseOnSurface,
            error = error, onError = onError,
            errorContainer = errorContainer, onErrorContainer = onErrorContainer,
            scrim = Color.Black,
        )
    } else {
        darkColorScheme(
            primary = primary, onPrimary = onPrimary,
            primaryContainer = primaryContainer, onPrimaryContainer = onPrimaryContainer,
            inversePrimary = hslColor(hue, s, 0.42f),
            secondary = secondary, onSecondary = onSecondary,
            secondaryContainer = secondaryContainer, onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary, onTertiary = onTertiary,
            tertiaryContainer = tertiaryContainer, onTertiaryContainer = onTertiaryContainer,
            background = background, onBackground = onBackground,
            surface = surface, onSurface = onSurface,
            surfaceVariant = surfaceVariant, onSurfaceVariant = onSurfaceVariant,
            surfaceTint = surfaceTint,
            surfaceBright = surfaceBright, surfaceDim = surfaceDim,
            surfaceContainer = surfaceContainer, surfaceContainerHigh = surfaceContainerHigh,
            surfaceContainerHighest = surfaceContainerHighest,
            surfaceContainerLow = surfaceContainerLow, surfaceContainerLowest = surfaceContainerLowest,
            outline = outline, outlineVariant = outlineVariant,
            inverseSurface = inverseSurface, inverseOnSurface = inverseOnSurface,
            error = error, onError = onError,
            errorContainer = errorContainer, onErrorContainer = onErrorContainer,
            scrim = Color.Black,
        )
    }
    return scheme
}

/** AMOLED variant: pure-black surfaces, like ZedSecure's amoled() extension. */
fun ColorScheme.applyAmoled(dark: Boolean): ColorScheme {
    if (!dark) return this
    return copy(
        background = Color.Black,
        surface = Color.Black,
        surfaceBright = Color(0xFF1A1A1A),
        surfaceDim = Color.Black,
        surfaceContainer = Color(0xFF0F0F0F),
        surfaceContainerHigh = Color(0xFF161616),
        surfaceContainerHighest = Color(0xFF1E1E1E),
        surfaceContainerLow = Color(0xFF0B0B0B),
        surfaceContainerLowest = Color.Black,
    )
}

/** Stock theme profiles (original designs): one tap applies accent + key colors. */
data class ThemeProfile(
    val id: String,
    val name: String,
    val accentIndex: Int,
    val amoled: Boolean = false,
    val connect: Long,
    val connected: Long,
    val serverCard: Long,
    val serverActive: Long,
    val configCard: Long,
    val configCardText: Long,
)

val ThemeProfiles: List<ThemeProfile> = listOf(
    ThemeProfile(
        id = "lagoon",
        name = "Lagoon",
        accentIndex = 2,
        connect = 0xFF00897E,
        connected = 0xFF00564F,
        serverCard = 0xFF0E2A28,
        serverActive = 0xFF0B3D3A,
        configCard = 0xFF0E2A28,
        configCardText = 0xFFD7F4F0,
    ),
    ThemeProfile(
        id = "sunset",
        name = "Sunset",
        accentIndex = 4,
        connect = 0xFFB4690E,
        connected = 0xFF7A3E00,
        serverCard = 0xFF2E1E08,
        serverActive = 0xFF3D2809,
        configCard = 0xFF2E1E08,
        configCardText = 0xFFFFEDD2,
    ),
    ThemeProfile(
        id = "onyx",
        name = "Onyx",
        accentIndex = 0,
        amoled = true,
        connect = 0xFF5646D6,
        connected = 0xFF2E2370,
        serverCard = 0xFF101014,
        serverActive = 0xFF1B1830,
        configCard = 0xFF14141A,
        configCardText = 0xFFE4E1FF,
    ),
)
