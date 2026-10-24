package com.vauth.foxyvpn.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import com.vauth.foxyvpn.ui.i18n.Text
import com.vauth.foxyvpn.ui.motion.screenMotion
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vauth.foxyvpn.data.SettingsStore
import com.vauth.foxyvpn.ui.theme.AccentPresets
import com.vauth.foxyvpn.ui.theme.AppearanceController
import com.vauth.foxyvpn.ui.theme.AppLanguage
import com.vauth.foxyvpn.ui.theme.CardCornerStyle
import com.vauth.foxyvpn.ui.theme.ColorContrast
import com.vauth.foxyvpn.ui.theme.ColorStyle
import com.vauth.foxyvpn.ui.theme.ConnectButtonStyle
import com.vauth.foxyvpn.ui.theme.ListDensity
import com.vauth.foxyvpn.ui.theme.NavBarStyle
import com.vauth.foxyvpn.ui.theme.PersonalizeSlot
import com.vauth.foxyvpn.ui.theme.ScreenTransition
import com.vauth.foxyvpn.ui.theme.ThemeMode
import com.vauth.foxyvpn.ui.theme.ThemeProfile
import com.vauth.foxyvpn.ui.theme.ThemeProfiles
import com.vauth.foxyvpn.ui.theme.UiFontScale
import com.vauth.foxyvpn.ui.theme.accentSeed
import kotlinx.coroutines.delay

/**
 * The Interface section. Its layout uses ZedSecure's settings page as a design
 * reference only.
 * Appearance only — the VPN core is untouched.
 */
@Composable
fun InterfaceSettingsSection(appearance: AppearanceController) {
    val cs = MaterialTheme.colorScheme
    var showDialog by remember { mutableStateOf<DialogKind?>(null) }

    SettingsSection(title = "Interface") {
        // --- Design -------------------------------------------------------
        Text(
            "Home screen design",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SettingsStore.AppDesign.entries.forEach { design ->
                DesignOptionCard(
                    label = design.label,
                    selected = appearance.appDesign == design,
                    onClick = { appearance.updateAppDesign(design) },
                    modifier = Modifier.weight(1f),
                ) { DesignPreview(design = design) }
            }
        }

        HorizontalDividerThin()

        // --- Colors -------------------------------------------------------
        SectionSubhead("Colors")
        ChoiceChipsRow(
            options = listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK),
            labels = listOf("System", "Light", "Dark"),
            selected = when (appearance.themeMode) {
                ThemeMode.AMOLED -> ThemeMode.SYSTEM
                else -> appearance.themeMode
            },
            onSelect = { appearance.updateThemeMode(it) },
        )
        SettingsSwitchRow(
            title = "Dynamic color",
            subtitle = "Follow your wallpaper colors (Android 12+)",
            checked = appearance.dynamicColor,
            onCheckedChange = { appearance.updateDynamicColor(it) },
        )
        if (!appearance.dynamicColor) {
            Text(
                "Color style",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 4.dp),
            ) {
                items(ColorStyle.entries) { style ->
                    val selected = appearance.colorStyle == style
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (selected) cs.primary else cs.surfaceContainerHighest,
                        contentColor = if (selected) cs.onPrimary else cs.onSurfaceVariant,
                        modifier = Modifier.selectable(selected = selected) {
                            appearance.updateColorStyle(style)
                        },
                    ) {
                        Text(
                            style.label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        )
                    }
                }
            }
            Text(
                "Accent color",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AccentPresets.forEachIndexed { index, preset ->
                    val selected = appearance.accentColorIndex == index
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(accentSeed(index, dark = false))
                            .border(
                                width = if (selected) 3.dp else 1.dp,
                                color = if (selected) cs.primary else cs.outlineVariant,
                                shape = CircleShape,
                            )
                            .clickable { appearance.updateAccentColorIndex(index) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = preset.name,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
            }
            ChoiceChipsRow(
                options = ColorContrast.entries,
                labels = ColorContrast.entries.map { it.label },
                selected = appearance.colorContrast,
                onSelect = { appearance.updateColorContrast(it) },
            )
        }
        SettingsSwitchRow(
            title = "AMOLED black",
            subtitle = "Pure black background to save battery on OLED screens",
            checked = appearance.themeMode == ThemeMode.AMOLED,
            onCheckedChange = { appearance.updateAmoled(it) },
        )

        HorizontalDividerThin()

        // --- Theme profiles -----------------------------------------------
        Text(
            "Theme profiles",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        Text(
            "One tap applies a curated look. Picking any color above returns to custom.",
            style = MaterialTheme.typography.bodySmall,
            color = cs.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ProfileCard(
                name = "Default",
                selected = appearance.themeProfileId == null,
                onClick = { appearance.applyThemeProfile(null) },
            ) { StockPreviewSwatches() }
            ThemeProfiles.forEach { profile ->
                ProfileCard(
                    name = profile.name,
                    selected = appearance.themeProfileId == profile.id,
                    onClick = { appearance.applyThemeProfile(profile) },
                ) { ProfileSwatches(profile) }
            }
        }

        HorizontalDividerThin()

        // --- Display ------------------------------------------------------
        SectionSubhead("Display")
        SettingsNavRowSimple(
            title = "Language",
            subtitle = when (appearance.appLanguage) {
                AppLanguage.System -> "System"
                AppLanguage.English -> "English"
                AppLanguage.Persian -> "فارسی"
            },
            onClick = { showDialog = DialogKind.Language },
        )
        SettingsNavRowSimple(
            title = "Font size",
            subtitle = appearance.uiFontScale.label,
            onClick = { showDialog = DialogKind.FontScale },
        )
        SettingsSwitchRow(
            title = "Reduce motion",
            subtitle = "Minimize animations across the app",
            checked = appearance.reduceMotion,
            onCheckedChange = { appearance.updateReduceMotion(it) },
        )
        SettingsSwitchRow(
            title = "Haptic feedback",
            subtitle = "Vibrate on connect and other key actions",
            checked = appearance.haptics,
            onCheckedChange = { appearance.updateHaptics(it) },
        )

        HorizontalDividerThin()

        // --- Screen transition --------------------------------------------
        Text(
            "Screen transition",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        TransitionPreview(
            style = appearance.screenTransition,
            reduceMotion = appearance.reduceMotion,
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 8.dp),
        ) {
            items(ScreenTransition.entries) { style ->
                val chosen = style == appearance.screenTransition
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (chosen) cs.primary else cs.surfaceContainerHighest,
                    contentColor = if (chosen) cs.onPrimary else cs.onSurfaceVariant,
                    modifier = Modifier.selectable(selected = chosen) {
                        appearance.updateScreenTransition(style)
                    },
                ) {
                    Text(
                        style.label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (chosen) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    )
                }
            }
        }

        HorizontalDividerThin()

        // --- Navigation bar ------------------------------------------------
        SettingsNavRowSimple(
            title = "Navigation bar style",
            subtitle = appearance.navBarStyle.label,
            onClick = { showDialog = DialogKind.NavBar },
        )

        // --- Connect button style ------------------------------------------
        Text(
            "Connect button style",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ConnectButtonStyle.entries.forEach { style ->
                val isSelected = style == appearance.connectButtonStyle
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(width = 88.dp, height = 66.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(cs.surfaceContainerHigh)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) cs.primary else cs.outlineVariant,
                                shape = RoundedCornerShape(14.dp),
                            )
                            .clickable { appearance.updateConnectButtonStyle(style) },
                        contentAlignment = Alignment.Center,
                    ) { ConnectStyleGlyph(style) }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        style.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }

        HorizontalDividerThin()

        // --- Personalize ---------------------------------------------------
        SectionSubhead("Personalize")
        PersonalizeSlot.entries.forEach { slot ->
            var showPicker by remember(slot) { mutableStateOf(false) }
            if (showPicker) {
                PersonalizeColorDialog(
                    title = slot.title,
                    current = slotCurrentColor(appearance, slot),
                    onDismiss = { showPicker = false },
                    onConfirm = { argb ->
                        appearance.updateCustomColor(slot, argb)
                        showPicker = false
                    },
                )
            }
            val current = slotCurrentColor(appearance, slot)
            SettingsNavRowSimple(
                title = slot.title,
                subtitle = if (current == null) "Default" else "Custom",
                trailing = {
                    Box(
                        Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(current ?: cs.primary)
                            .border(1.dp, cs.outlineVariant, CircleShape),
                    )
                },
                onClick = { showPicker = true },
            )
        }
        SettingsNavRowSimple(
            title = "Card corners",
            subtitle = appearance.cardCornerStyle.label,
            onClick = { showDialog = DialogKind.Corners },
        )
        SettingsNavRowSimple(
            title = "List density",
            subtitle = appearance.listDensity.label,
            onClick = { showDialog = DialogKind.Density },
        )
        SettingsSwitchRow(
            title = "Show ping on locations",
            subtitle = "Measure latency next to each location",
            checked = appearance.showServerPing,
            onCheckedChange = { appearance.updateShowServerPing(it) },
        )
        SettingsSwitchRow(
            title = "Show server counts",
            subtitle = "Show how many servers each location has",
            checked = appearance.showServerUsage,
            onCheckedChange = { appearance.updateShowServerUsage(it) },
        )
        SettingsSwitchRow(
            title = "Monospace addresses",
            subtitle = "Use a fixed-width font for server addresses",
            checked = appearance.monospaceAddress,
            onCheckedChange = { appearance.updateMonospaceAddress(it) },
        )
        if (appearance.hasPersonalization()) {
            TextButton(
                onClick = { appearance.resetPersonalization() },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Text("Reset all personalization")
            }
        }

        HorizontalDividerThin()

        // --- Behaviour ------------------------------------------------------
        SectionSubhead("Behaviour")
        SettingsSwitchRow(
            title = "Confirm before removing",
            subtitle = "Ask before deleting an account",
            checked = appearance.confirmRemove,
            onCheckedChange = { appearance.updateConfirmRemove(it) },
        )
        SettingsSwitchRow(
            title = "Back home after selecting",
            subtitle = "Return to the home screen after picking a location",
            checked = appearance.homeAfterSelect,
            onCheckedChange = { appearance.updateHomeAfterSelect(it) },
        )
        SettingsSwitchRow(
            title = "Two-column locations",
            subtitle = "Show the location list in two columns",
            checked = appearance.doubleColumnDisplay,
            onCheckedChange = { appearance.updateDoubleColumnDisplay(it) },
        )
    }

    // --- Option dialogs ----------------------------------------------------
    when (showDialog) {
        DialogKind.Language -> OptionsDialog(
            title = "Language",
            options = AppLanguage.entries.map { it to it.label },
            selected = appearance.appLanguage,
            onDismiss = { showDialog = null },
            onSelect = {
                appearance.updateAppLanguage(it)
                showDialog = null
            },
        )
        DialogKind.FontScale -> OptionsDialog(
            title = "Font size",
            options = UiFontScale.entries.map { it to it.label },
            selected = appearance.uiFontScale,
            onDismiss = { showDialog = null },
            onSelect = {
                appearance.updateUiFontScale(it)
                showDialog = null
            },
        )
        DialogKind.NavBar -> OptionsDialog(
            title = "Navigation bar style",
            options = NavBarStyle.entries.map { it to it.label },
            selected = appearance.navBarStyle,
            onDismiss = { showDialog = null },
            onSelect = {
                appearance.updateNavBarStyle(it)
                showDialog = null
            },
        )
        DialogKind.Corners -> OptionsDialog(
            title = "Card corners",
            options = CardCornerStyle.entries.map { it to it.label },
            selected = appearance.cardCornerStyle,
            onDismiss = { showDialog = null },
            onSelect = {
                appearance.updateCardCornerStyle(it)
                showDialog = null
            },
        )
        DialogKind.Density -> OptionsDialog(
            title = "List density",
            options = ListDensity.entries.map { it to it.label },
            selected = appearance.listDensity,
            onDismiss = { showDialog = null },
            onSelect = {
                appearance.updateListDensity(it)
                showDialog = null
            },
        )
        null -> Unit
    }
}

private enum class DialogKind { Language, FontScale, NavBar, Corners, Density }

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

@Composable
private fun SectionSubhead(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmallEmphasized,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
    )
}

@Composable
private fun HorizontalDividerThin() {
    androidx.compose.material3.HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    )
}

@Composable
private fun <T> ChoiceChipsRow(
    options: List<T>,
    labels: List<String>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        items(options.size) { i ->
            val chosen = options[i] == selected
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (chosen) cs.primary else cs.surfaceContainerHighest,
                contentColor = if (chosen) cs.onPrimary else cs.onSurfaceVariant,
                modifier = Modifier.selectable(selected = chosen) { onSelect(options[i]) },
            ) {
                Text(
                    labels[i],
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (chosen) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                )
            }
        }
    }
}

@Composable
private fun SettingsNavRowSimple(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = null,
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant,
                maxLines = 1,
            )
        }
        Spacer(Modifier.width(12.dp))
        trailing?.invoke()
    }
}

@Composable
private fun ProfileCard(
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
    preview: @Composable () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(width = 84.dp, height = 120.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(cs.surfaceContainerHigh)
                .border(
                    width = if (selected) 3.dp else 1.dp,
                    color = if (selected) cs.primary else cs.outlineVariant,
                    shape = RoundedCornerShape(16.dp),
                )
                .clickable(onClick = onClick)
                .padding(8.dp),
        ) { preview() }
        Spacer(Modifier.height(6.dp))
        Text(
            name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun StockPreviewSwatches() {
    val cs = MaterialTheme.colorScheme
    MiniProfilePreview(
        hero = cs.primary,
        a = cs.tertiary,
        b = cs.secondary,
        card = cs.primaryContainer,
    )
}

@Composable
private fun ProfileSwatches(profile: ThemeProfile) {
    MiniProfilePreview(
        hero = Color(profile.connect),
        a = Color(profile.serverActive),
        b = Color(profile.connected),
        card = Color(profile.configCard),
    )
}

@Composable
private fun MiniProfilePreview(hero: Color, a: Color, b: Color, card: Color) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(hero))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Box(Modifier.weight(1f).height(28.dp).clip(RoundedCornerShape(7.dp)).background(a))
            Box(Modifier.weight(1f).height(28.dp).clip(RoundedCornerShape(7.dp)).background(b))
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(22.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(card),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(16.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(hero),
        )
    }
}

@Composable
private fun ConnectStyleGlyph(style: ConnectButtonStyle) {
    val accent = MaterialTheme.colorScheme.primary
    val on = MaterialTheme.colorScheme.onPrimary
    when (style) {
        ConnectButtonStyle.Pill -> Box(
            Modifier
                .size(width = 58.dp, height = 22.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(accent),
        )
        ConnectButtonStyle.Hero -> Box(
            Modifier.size(38.dp).clip(CircleShape).background(accent),
            contentAlignment = Alignment.Center,
        ) { Text("F", color = on, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        ConnectButtonStyle.Ring -> Box(
            Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(accent)
                .border(3.dp, on.copy(alpha = 0.5f), CircleShape),
        )
        ConnectButtonStyle.Bar -> Box(
            Modifier
                .size(width = 62.dp, height = 14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(accent),
        )
        ConnectButtonStyle.Icon -> Box(
            Modifier.size(30.dp).clip(RoundedCornerShape(10.dp)).background(accent),
        )
        ConnectButtonStyle.Switch -> Box(
            Modifier
                .size(width = 58.dp, height = 26.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(accent),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                Modifier
                    .padding(start = 4.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(on.copy(alpha = 0.6f)),
            )
        }
    }
}

/** Animated preview of the chosen screen transition. */
@Composable
private fun TransitionPreview(style: ScreenTransition, reduceMotion: Boolean) {
    var step by remember { mutableIntStateOf(0) }
    LaunchedEffect(style) {
        step = 0
        while (true) {
            delay(1700)
            step++
        }
    }
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = cs.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(128.dp),
    ) {
        Box(Modifier.padding(10.dp)) {
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    transitionTransform(style, reduceMotion).using(SizeTransform(clip = false))
                },
                label = "transition-preview",
                modifier = Modifier.fillMaxSize(),
            ) { value ->
                ToyScreen(index = value, modifier = previewDecoration(style, value, reduceMotion))
            }
        }
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun ToyScreen(index: Int, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val accent = if (index % 2 == 0) cs.primary else cs.tertiary
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cs.surfaceContainerHighest,
        modifier = modifier.fillMaxSize(),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(22.dp).clip(CircleShape).background(accent))
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .height(10.dp)
                        .width(90.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.5f)),
                )
            }
            repeat(3) { row ->
                Box(
                    Modifier
                        .fillMaxWidth(if (row == 1) 0.72f else 0.92f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(cs.surfaceContainer),
                )
            }
        }
    }
}

/** Forward-navigation transition for [style]; the preview in settings uses the same code as the nav graph. */
fun transitionTransform(
    style: ScreenTransition,
    reduceMotion: Boolean,
): androidx.compose.animation.ContentTransform {
    val motion = screenMotion(style, reduceMotion, rtl = false)
    return motion.enter togetherWith motion.exit
}

@Composable
private fun previewDecoration(
    style: ScreenTransition,
    value: Int,
    reduceMotion: Boolean,
): Modifier {
    if (reduceMotion) return Modifier
    return when (style) {
        ScreenTransition.Flip -> Modifier.graphicsLayer {
            rotationY = (value % 2) * 12f
        }
        else -> Modifier
    }
}

@Composable
private fun <T> OptionsDialog(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onDismiss: () -> Unit,
    onSelect: (T) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { (value, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(value) }
                            .padding(vertical = 8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .border(
                                    2.dp,
                                    if (value == selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant,
                                    CircleShape,
                                )
                                .background(
                                    if (value == selected) MaterialTheme.colorScheme.primary
                                    else Color.Transparent,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (value == selected) {
                                androidx.compose.material3.Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
private fun PersonalizeColorDialog(
    title: String,
    current: Color?,
    onDismiss: () -> Unit,
    onConfirm: (Long?) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    var hex by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // "Default" swatch.
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(cs.surfaceContainerHighest)
                            .border(
                                2.dp,
                                if (current == null) cs.primary else cs.outlineVariant,
                                CircleShape,
                            )
                            .clickable { onConfirm(null) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("A", fontSize = 11.sp, color = cs.onSurfaceVariant)
                    }
                    AccentPresets.forEachIndexed { index, preset ->
                        val color = accentSeed(index, dark = false)
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    2.dp,
                                    if (current == color) cs.primary else cs.outlineVariant,
                                    CircleShape,
                                )
                                .clickable { onConfirm(color.toArgbLong()) },
                        )
                    }
                }
                OutlinedTextField(
                    value = hex,
                    onValueChange = {
                        hex = it
                        error = false
                    },
                    label = { Text("Custom hex (e.g. #4A90D9)") },
                    singleLine = true,
                    isError = error,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val parsed = parseHexColor(hex)
                    if (parsed != null) onConfirm(parsed) else error = true
                },
            ) { Text("Apply") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

private fun Color.toArgbLong(): Long {
    val a = (alpha * 255).toInt()
    val r = (red * 255).toInt()
    val g = (green * 255).toInt()
    val b = (blue * 255).toInt()
    return ((a.toLong() shl 24) or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong()) and 0xFFFFFFFFL
}

private fun parseHexColor(input: String): Long? {
    val hex = input.trim().removePrefix("#")
    if (hex.length != 6 && hex.length != 8) return null
    return runCatching {
        val full = if (hex.length == 6) "FF$hex" else hex
        full.toLong(16) and 0xFFFFFFFFL
    }.getOrNull()
}

private fun slotCurrentColor(appearance: AppearanceController, slot: PersonalizeSlot): Color? {
    val p = appearance.personalization
    return when (slot) {
        PersonalizeSlot.Connect -> p.connectColor
        PersonalizeSlot.Connected -> p.connectedColor
        PersonalizeSlot.ServerCard -> p.serverCardColor
        PersonalizeSlot.ServerActive -> p.serverActiveColor
        PersonalizeSlot.ConfigCard -> p.configCardColor
        PersonalizeSlot.ConfigCardText -> p.configCardTextColor
    }
}
