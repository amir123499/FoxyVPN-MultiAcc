package com.vauth.foxyvpn.ui.theme

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.vauth.foxyvpn.ui.screens.LocalMotionBudget
import com.vauth.foxyvpn.ui.screens.MotionBudget

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

    // Throttle idle animations when Android Battery Saver is on.
    val motionBudget = rememberMotionBudget(context)

    // Cross-fade theme/accent/contrast/AMOLED changes instead of snapping.
    val animatedScheme = animateColorScheme(colorScheme)

    CompositionLocalProvider(
        LocalMotionBudget provides motionBudget,
        LocalFoxyStatusColors provides when {
            amoled -> AmoledStatusColors
            darkTheme -> DarkStatusColors
            else -> LightStatusColors
        },
    ) {
        MaterialExpressiveTheme(
            colorScheme = animatedScheme,
            typography = FoxyTypography,
            motionScheme = MotionScheme.expressive(),
            content = content,
        )
    }
}

@Composable
private fun rememberMotionBudget(context: Context): MotionBudget {
    val pm = remember(context) {
        context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    }
    // Re-read when the composable is entered; battery saver is sticky enough
    // that a one-shot sample is acceptable (no continuous broadcast needed).
    val saveOn = pm?.isPowerSaveMode == true
    return if (saveOn) MotionBudget.Throttled else MotionBudget.Full
}

/**
 * Animates each ColorScheme role so theme/accent/contrast/AMOLED switches
 * cross-fade rather than snap. Duration is short so it still feels responsive.
 */
@Composable
private fun animateColorScheme(target: ColorScheme): ColorScheme {
    val d = 280
    val primary by animateColorAsState(target.primary, tween(d), label = "primary")
    val onPrimary by animateColorAsState(target.onPrimary, tween(d), label = "onPrimary")
    val primaryContainer by animateColorAsState(target.primaryContainer, tween(d), label = "primaryContainer")
    val onPrimaryContainer by animateColorAsState(target.onPrimaryContainer, tween(d), label = "onPrimaryContainer")
    val secondary by animateColorAsState(target.secondary, tween(d), label = "secondary")
    val onSecondary by animateColorAsState(target.onSecondary, tween(d), label = "onSecondary")
    val secondaryContainer by animateColorAsState(target.secondaryContainer, tween(d), label = "secondaryContainer")
    val onSecondaryContainer by animateColorAsState(target.onSecondaryContainer, tween(d), label = "onSecondaryContainer")
    val tertiary by animateColorAsState(target.tertiary, tween(d), label = "tertiary")
    val onTertiary by animateColorAsState(target.onTertiary, tween(d), label = "onTertiary")
    val tertiaryContainer by animateColorAsState(target.tertiaryContainer, tween(d), label = "tertiaryContainer")
    val onTertiaryContainer by animateColorAsState(target.onTertiaryContainer, tween(d), label = "onTertiaryContainer")
    val error by animateColorAsState(target.error, tween(d), label = "error")
    val onError by animateColorAsState(target.onError, tween(d), label = "onError")
    val errorContainer by animateColorAsState(target.errorContainer, tween(d), label = "errorContainer")
    val onErrorContainer by animateColorAsState(target.onErrorContainer, tween(d), label = "onErrorContainer")
    val background by animateColorAsState(target.background, tween(d), label = "background")
    val onBackground by animateColorAsState(target.onBackground, tween(d), label = "onBackground")
    val surface by animateColorAsState(target.surface, tween(d), label = "surface")
    val onSurface by animateColorAsState(target.onSurface, tween(d), label = "onSurface")
    val surfaceVariant by animateColorAsState(target.surfaceVariant, tween(d), label = "surfaceVariant")
    val onSurfaceVariant by animateColorAsState(target.onSurfaceVariant, tween(d), label = "onSurfaceVariant")
    val outline by animateColorAsState(target.outline, tween(d), label = "outline")
    val outlineVariant by animateColorAsState(target.outlineVariant, tween(d), label = "outlineVariant")
    val inverseSurface by animateColorAsState(target.inverseSurface, tween(d), label = "inverseSurface")
    val inverseOnSurface by animateColorAsState(target.inverseOnSurface, tween(d), label = "inverseOnSurface")
    val inversePrimary by animateColorAsState(target.inversePrimary, tween(d), label = "inversePrimary")
    val surfaceTint by animateColorAsState(target.surfaceTint, tween(d), label = "surfaceTint")
    val scrim by animateColorAsState(target.scrim, tween(d), label = "scrim")
    val surfaceBright by animateColorAsState(target.surfaceBright, tween(d), label = "surfaceBright")
    val surfaceDim by animateColorAsState(target.surfaceDim, tween(d), label = "surfaceDim")
    val surfaceContainer by animateColorAsState(target.surfaceContainer, tween(d), label = "surfaceContainer")
    val surfaceContainerHigh by animateColorAsState(target.surfaceContainerHigh, tween(d), label = "surfaceContainerHigh")
    val surfaceContainerHighest by animateColorAsState(target.surfaceContainerHighest, tween(d), label = "surfaceContainerHighest")
    val surfaceContainerLow by animateColorAsState(target.surfaceContainerLow, tween(d), label = "surfaceContainerLow")
    val surfaceContainerLowest by animateColorAsState(target.surfaceContainerLowest, tween(d), label = "surfaceContainerLowest")
    return target.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        outline = outline,
        outlineVariant = outlineVariant,
        inverseSurface = inverseSurface,
        inverseOnSurface = inverseOnSurface,
        inversePrimary = inversePrimary,
        surfaceTint = surfaceTint,
        scrim = scrim,
        surfaceBright = surfaceBright,
        surfaceDim = surfaceDim,
        surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surfaceContainerHigh,
        surfaceContainerHighest = surfaceContainerHighest,
        surfaceContainerLow = surfaceContainerLow,
        surfaceContainerLowest = surfaceContainerLowest,
    )
}
