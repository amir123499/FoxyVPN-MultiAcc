package com.vauth.foxyvpn.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = px_light_primary,
    onPrimary = px_light_onPrimary,
    primaryContainer = px_light_primaryContainer,
    onPrimaryContainer = px_light_onPrimaryContainer,
    inversePrimary = px_light_inversePrimary,
    secondary = px_light_secondary,
    onSecondary = px_light_onSecondary,
    secondaryContainer = px_light_secondaryContainer,
    onSecondaryContainer = px_light_onSecondaryContainer,
    tertiary = px_light_tertiary,
    onTertiary = px_light_onTertiary,
    tertiaryContainer = px_light_tertiaryContainer,
    onTertiaryContainer = px_light_onTertiaryContainer,
    error = px_light_error,
    onError = px_light_onError,
    errorContainer = px_light_errorContainer,
    onErrorContainer = px_light_onErrorContainer,
    background = px_light_background,
    onBackground = px_light_onBackground,
    surface = px_light_surface,
    onSurface = px_light_onSurface,
    surfaceVariant = px_light_surfaceVariant,
    onSurfaceVariant = px_light_onSurfaceVariant,
    surfaceTint = px_light_surfaceTint,
    surfaceBright = px_light_surfaceBright,
    surfaceDim = px_light_surfaceDim,
    surfaceContainer = px_light_surfaceContainer,
    surfaceContainerHigh = px_light_surfaceContainerHigh,
    surfaceContainerHighest = px_light_surfaceContainerHighest,
    surfaceContainerLow = px_light_surfaceContainerLow,
    surfaceContainerLowest = px_light_surfaceContainerLowest,
    outline = px_light_outline,
    outlineVariant = px_light_outlineVariant,
    inverseSurface = px_light_inverseSurface,
    inverseOnSurface = px_light_inverseOnSurface,
    scrim = px_light_scrim,
)

private val DarkColors = darkColorScheme(
    primary = px_dark_primary,
    onPrimary = px_dark_onPrimary,
    primaryContainer = px_dark_primaryContainer,
    onPrimaryContainer = px_dark_onPrimaryContainer,
    inversePrimary = px_dark_inversePrimary,
    secondary = px_dark_secondary,
    onSecondary = px_dark_onSecondary,
    secondaryContainer = px_dark_secondaryContainer,
    onSecondaryContainer = px_dark_onSecondaryContainer,
    tertiary = px_dark_tertiary,
    onTertiary = px_dark_onTertiary,
    tertiaryContainer = px_dark_tertiaryContainer,
    onTertiaryContainer = px_dark_onTertiaryContainer,
    error = px_dark_error,
    onError = px_dark_onError,
    errorContainer = px_dark_errorContainer,
    onErrorContainer = px_dark_onErrorContainer,
    background = px_dark_background,
    onBackground = px_dark_onBackground,
    surface = px_dark_surface,
    onSurface = px_dark_onSurface,
    surfaceVariant = px_dark_surfaceVariant,
    onSurfaceVariant = px_dark_onSurfaceVariant,
    surfaceTint = px_dark_surfaceTint,
    surfaceBright = px_dark_surfaceBright,
    surfaceDim = px_dark_surfaceDim,
    surfaceContainer = px_dark_surfaceContainer,
    surfaceContainerHigh = px_dark_surfaceContainerHigh,
    surfaceContainerHighest = px_dark_surfaceContainerHighest,
    surfaceContainerLow = px_dark_surfaceContainerLow,
    surfaceContainerLowest = px_dark_surfaceContainerLowest,
    outline = px_dark_outline,
    outlineVariant = px_dark_outlineVariant,
    inverseSurface = px_dark_inverseSurface,
    inverseOnSurface = px_dark_inverseOnSurface,
    scrim = px_dark_scrim,
)

private val AmoledColors = darkColorScheme(
    primary = px_dark_primary,
    onPrimary = px_dark_onPrimary,
    primaryContainer = px_dark_primaryContainer,
    onPrimaryContainer = px_dark_onPrimaryContainer,
    inversePrimary = px_dark_inversePrimary,
    secondary = px_dark_secondary,
    onSecondary = px_dark_onSecondary,
    secondaryContainer = px_dark_secondaryContainer,
    onSecondaryContainer = px_dark_onSecondaryContainer,
    tertiary = px_dark_tertiary,
    onTertiary = px_dark_onTertiary,
    tertiaryContainer = px_dark_tertiaryContainer,
    onTertiaryContainer = px_dark_onTertiaryContainer,
    error = px_dark_error,
    onError = px_dark_onError,
    errorContainer = px_dark_errorContainer,
    onErrorContainer = px_dark_onErrorContainer,
    background = px_amoled_background,
    onBackground = px_dark_onBackground,
    surface = px_amoled_surface,
    onSurface = px_dark_onSurface,
    surfaceVariant = px_amoled_surfaceVariant,
    onSurfaceVariant = px_dark_onSurfaceVariant,
    surfaceTint = px_dark_surfaceTint,
    surfaceBright = px_amoled_surfaceBright,
    surfaceDim = px_amoled_surfaceDim,
    surfaceContainer = px_amoled_surfaceContainer,
    surfaceContainerHigh = px_amoled_surfaceContainerHigh,
    surfaceContainerHighest = px_amoled_surfaceContainerHighest,
    surfaceContainerLow = px_amoled_surfaceContainerLow,
    surfaceContainerLowest = px_amoled_surfaceContainerLowest,
    outline = px_dark_outline,
    outlineVariant = px_dark_outlineVariant,
    inverseSurface = px_amoled_inverseSurface,
    inverseOnSurface = px_amoled_inverseOnSurface,
    scrim = px_dark_scrim,
)

@Composable
fun FoxyVpnTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    colorStyle: ColorStyle = ColorStyle.ZedSecure,
    accentColorIndex: Int = 0,
    colorContrast: ColorContrast = ColorContrast.Standard,
    content: @Composable () -> Unit,
) {
    val systemInDarkTheme = isSystemInDarkTheme()
    val amoled = themeMode == ThemeMode.AMOLED
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> systemInDarkTheme
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.AMOLED -> true
    }
    val context = LocalContext.current
    val useDynamic = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val colorScheme = when {
        useDynamic -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> {
            val seed = accentSeed(accentColorIndex, darkTheme)
            seedColorScheme(
                dark = darkTheme,
                seed = seed,
                style = colorStyle,
                contrast = colorContrast,
            ).let { if (amoled) it.applyAmoled(darkTheme) else it }
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalFoxyStatusColors provides when {
            amoled -> AmoledStatusColors
            darkTheme -> DarkStatusColors
            else -> LightStatusColors
        },
    ) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            typography = FoxyTypography,
            motionScheme = MotionScheme.expressive(),
            content = content,
        )
    }
}
