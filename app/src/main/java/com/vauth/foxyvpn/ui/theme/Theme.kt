package com.vauth.foxyvpn.ui.theme

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.PowerManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.snap
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
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
    reduceMotion: Boolean = false,
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
    val targetScheme = when {
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
    // Cross-fade between schemes so changing theme, accent, contrast or AMOLED
    // never snaps; with reduce motion the change is instant.
    val colorScheme = animatedColorScheme(targetScheme, animate = !reduceMotion)
    val motionBudget = rememberMotionBudget()

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
        LocalMotionBudget provides motionBudget,
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

/** Returns [target] with every base colour animated toward its new value. */
@Composable
private fun animatedColorScheme(target: ColorScheme, animate: Boolean): ColorScheme {
    val spec: AnimationSpec<Color> = if (animate) tween<Color>(durationMillis = 420) else snap<Color>()

    @Composable
    fun anim(color: Color): Color = animateColorAsState(color, spec, label = "scheme").value

    return target.copy(
        primary = anim(target.primary),
        onPrimary = anim(target.onPrimary),
        primaryContainer = anim(target.primaryContainer),
        onPrimaryContainer = anim(target.onPrimaryContainer),
        inversePrimary = anim(target.inversePrimary),
        secondary = anim(target.secondary),
        onSecondary = anim(target.onSecondary),
        secondaryContainer = anim(target.secondaryContainer),
        onSecondaryContainer = anim(target.onSecondaryContainer),
        tertiary = anim(target.tertiary),
        onTertiary = anim(target.onTertiary),
        tertiaryContainer = anim(target.tertiaryContainer),
        onTertiaryContainer = anim(target.onTertiaryContainer),
        background = anim(target.background),
        onBackground = anim(target.onBackground),
        surface = anim(target.surface),
        onSurface = anim(target.onSurface),
        surfaceVariant = anim(target.surfaceVariant),
        onSurfaceVariant = anim(target.onSurfaceVariant),
        surfaceTint = anim(target.surfaceTint),
        inverseSurface = anim(target.inverseSurface),
        inverseOnSurface = anim(target.inverseOnSurface),
        error = anim(target.error),
        onError = anim(target.onError),
        errorContainer = anim(target.errorContainer),
        onErrorContainer = anim(target.onErrorContainer),
        outline = anim(target.outline),
        outlineVariant = anim(target.outlineVariant),
        scrim = anim(target.scrim),
        surfaceBright = anim(target.surfaceBright),
        surfaceDim = anim(target.surfaceDim),
        surfaceContainer = anim(target.surfaceContainer),
        surfaceContainerHigh = anim(target.surfaceContainerHigh),
        surfaceContainerHighest = anim(target.surfaceContainerHighest),
        surfaceContainerLow = anim(target.surfaceContainerLow),
        surfaceContainerLowest = anim(target.surfaceContainerLowest),
    )
}

/**
 * Full motion normally; while Android's battery saver is on, looping
 * animations stop and only the ones that show progress keep running.
 */
@Composable
private fun rememberMotionBudget(): MotionBudget {
    val context = LocalContext.current
    val powerManager = remember(context) { context.getSystemService(Context.POWER_SERVICE) as? PowerManager }
    var saverOn by remember { mutableStateOf(powerManager?.isPowerSaveMode == true) }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                saverOn = powerManager?.isPowerSaveMode == true
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        onDispose { context.unregisterReceiver(receiver) }
    }
    return if (saverOn) MotionBudget.Throttled else MotionBudget.Full
}
