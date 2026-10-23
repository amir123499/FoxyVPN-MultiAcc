package com.vauth.foxyvpn.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vauth.foxyvpn.data.model.ConnectionState
import com.vauth.foxyvpn.ui.theme.ConnectButtonStyle
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * How much motion the app may run right now. Provided by the theme.
 * Full runs everything, Throttled only animates while connecting,
 * Paused stops all looping animation.
 */
enum class MotionBudget { Full, Throttled, Paused }

val LocalMotionBudget = compositionLocalOf { MotionBudget.Full }

/**
 * The connect control shown on the home screen. Six presentations share
 * one state model: the hero tile (a rounded square whose corners round off
 * when the tunnel is up, with an orbiting arc while connecting and a slow
 * halo while connected) and five compact controls.
 *
 * Written for this app from scratch; it does not reuse code from other
 * projects.
 */
@Composable
fun ConnectCore(
    state: ConnectionState,
    style: ConnectButtonStyle = ConnectButtonStyle.Hero,
    reduceMotion: Boolean = false,
    connectColor: Color? = null,
    connectedColor: Color? = null,
    onToggle: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val live = state == ConnectionState.CONNECTED
    val busy = state == ConnectionState.CONNECTING
    val budget = LocalMotionBudget.current
    val animate = !reduceMotion && when (budget) {
        MotionBudget.Full -> true
        MotionBudget.Throttled -> busy
        MotionBudget.Paused -> false
    }
    val idleAccent = connectColor ?: cs.primary
    val liveAccent = connectedColor ?: cs.primary
    val haptics = LocalHapticFeedback.current
    val label = stateLabel(state)

    var previous by remember { mutableStateOf(state) }
    // 0 → 1 drives the one-shot ring that plays when the tunnel comes up.
    // Starts at 1 so no ring is visible until a connection completes.
    val burst = remember { Animatable(1f) }
    LaunchedEffect(state) {
        if (live && previous == ConnectionState.CONNECTING) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            if (animate) {
                burst.snapTo(0f)
                burst.animateTo(1f, tween(durationMillis = 900, easing = LinearEasing))
            }
        }
        previous = state
    }

    // Seconds since the looping clock started. It is read only inside draw
    // blocks, so updating it redraws without recomposing.
    val clock = remember { mutableFloatStateOf(0f) }
    val loopNeeded = style == ConnectButtonStyle.Hero && animate && (live || busy)
    LaunchedEffect(loopNeeded) {
        if (!loopNeeded) return@LaunchedEffect
        val origin = withFrameNanos { it }
        while (true) {
            withFrameNanos { now -> clock.floatValue = (now - origin) / 1_000_000_000f }
        }
    }

    when (style) {
        ConnectButtonStyle.Hero -> HeroTile(
            live = live,
            busy = busy,
            animate = animate,
            idleAccent = idleAccent,
            liveAccent = liveAccent,
            label = label,
            clock = { clock.floatValue },
            burst = { burst.value },
            onTap = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onToggle()
            },
        )
        ConnectButtonStyle.Ring -> RingControl(live, busy, idleAccent, liveAccent, label, onToggle)
        ConnectButtonStyle.Bar -> BarControl(live, busy, idleAccent, liveAccent, label, onToggle)
        ConnectButtonStyle.Icon -> IconControl(live, busy, idleAccent, liveAccent, label, onToggle)
        ConnectButtonStyle.Switch -> SwitchControl(live, busy, label, onToggle)
        ConnectButtonStyle.Pill -> PillControl(live, busy, idleAccent, liveAccent, label, onToggle)
    }
}

private fun stateLabel(state: ConnectionState): String = when (state) {
    ConnectionState.DISCONNECTED -> "Connect"
    ConnectionState.CONNECTING -> "Connecting…"
    ConnectionState.CONNECTED -> "Disconnect"
}

/** Black or white, whichever reads better on the given fill colour. */
private fun onColorFor(fill: Color): Color =
    if (fill.luminance() > 0.5f) Color.Black else Color.White

// ─── Hero tile ───────────────────────────────────────────────────────────────

@Composable
private fun HeroTile(
    live: Boolean,
    busy: Boolean,
    animate: Boolean,
    idleAccent: Color,
    liveAccent: Color,
    label: String,
    clock: () -> Float,
    burst: () -> Float,
    onTap: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    BoxWithConstraints(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val tile = (maxWidth * 0.5f).coerceIn(120.dp, 200.dp)
        val outer = tile * 1.3f

        // Corners go from a soft square to a near-circle when connected.
        val cornerPercent by animateFloatAsState(
            targetValue = if (live) 50f else 28f,
            animationSpec = spring(dampingRatio = 0.75f, stiffness = 350f),
            label = "tile-corner",
        )
        val fill by animateColorAsState(
            targetValue = if (live) liveAccent else cs.surfaceContainerHighest,
            label = "tile-fill",
        )
        val glyph by animateColorAsState(
            targetValue = if (live) onColorFor(liveAccent) else idleAccent,
            label = "tile-glyph",
        )

        Box(modifier = Modifier.size(outer), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val tilePx = tile.toPx()
                val center = Offset(size.width / 2f, size.height / 2f)
                val t = clock()
                if (animate && live) {
                    // Slow breathing halo behind the tile.
                    val breathe = 0.5f + 0.5f * sin(2 * PI * t / 2.4).toFloat()
                    drawCircle(
                        color = liveAccent.copy(alpha = 0.10f + 0.10f * breathe),
                        radius = tilePx * (0.62f + 0.06f * breathe),
                        center = center,
                    )
                }
                if (animate && busy) {
                    // An arc that orbits the tile while the tunnel is starting.
                    val sweepStart = (t * 360f / 1.6f) % 360f
                    val inset = (size.width - tilePx) / 2f - 6.dp.toPx()
                    drawArc(
                        color = idleAccent,
                        startAngle = sweepStart,
                        sweepAngle = 96f,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = Size(size.width - 2 * inset, size.height - 2 * inset),
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
                    )
                }
                val b = burst()
                if (b < 1f) {
                    // One expanding ring when the connection completes.
                    drawCircle(
                        color = liveAccent.copy(alpha = (1f - b) * 0.5f),
                        radius = tilePx * (0.55f + 0.55f * b),
                        center = center,
                        style = Stroke(width = 3.dp.toPx()),
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(tile)
                    .clip(RoundedCornerShape(percent = cornerPercent.roundToInt()))
                    .background(fill)
                    .semantics { contentDescription = label }
                    .clickable(role = Role.Button, onClick = onTap),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.size(tile * 0.4f)) {
                    drawPowerGlyph(glyph)
                }
            }
        }
    }
}

/** A power symbol: an open ring with a short stem through the gap. */
private fun DrawScope.drawPowerGlyph(color: Color) {
    val s = size.minDimension
    val stroke = s * 0.11f
    val r = s * 0.36f
    val center = Offset(size.width / 2f, size.height / 2f)
    drawArc(
        color = color,
        startAngle = -60f,
        sweepAngle = 300f,
        useCenter = false,
        topLeft = Offset(center.x - r, center.y - r),
        size = Size(r * 2f, r * 2f),
        style = Stroke(width = stroke, cap = StrokeCap.Round),
    )
    drawLine(
        color = color,
        start = Offset(center.x, center.y - r * 1.3f),
        end = Offset(center.x, center.y - r * 0.05f),
        strokeWidth = stroke,
        cap = StrokeCap.Round,
    )
}

// ─── Compact controls ────────────────────────────────────────────────────────

@Composable
private fun RingControl(
    live: Boolean,
    busy: Boolean,
    idleAccent: Color,
    liveAccent: Color,
    label: String,
    onToggle: () -> Unit,
) {
    val accent = if (live) liveAccent else idleAccent
    Box(
        modifier = Modifier
            .size(104.dp)
            .border(width = 3.dp, color = accent, shape = CircleShape)
            .clip(CircleShape)
            .background(if (live) liveAccent.copy(alpha = 0.12f) else Color.Transparent)
            .semantics { contentDescription = label }
            .clickable(role = Role.Button, enabled = !busy, onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = accent,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun BarControl(
    live: Boolean,
    busy: Boolean,
    idleAccent: Color,
    liveAccent: Color,
    label: String,
    onToggle: () -> Unit,
) {
    val fill = if (live) liveAccent else idleAccent
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(fill)
            .semantics { contentDescription = label }
            .clickable(role = Role.Button, enabled = !busy, onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, color = onColorFor(fill), style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun IconControl(
    live: Boolean,
    busy: Boolean,
    idleAccent: Color,
    liveAccent: Color,
    label: String,
    onToggle: () -> Unit,
) {
    val fill = if (live) liveAccent else idleAccent
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(fill)
            .semantics { contentDescription = label }
            .clickable(role = Role.Button, enabled = !busy, onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(30.dp)) { drawPowerGlyph(onColorFor(fill)) }
    }
}

@Composable
private fun SwitchControl(
    live: Boolean,
    busy: Boolean,
    label: String,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.titleMedium)
        Switch(
            checked = live || busy,
            enabled = !busy,
            onCheckedChange = { onToggle() },
        )
    }
}

@Composable
private fun PillControl(
    live: Boolean,
    busy: Boolean,
    idleAccent: Color,
    liveAccent: Color,
    label: String,
    onToggle: () -> Unit,
) {
    val fill = if (live) liveAccent else idleAccent
    Box(
        modifier = Modifier
            .widthIn(min = 160.dp)
            .height(48.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(fill)
            .padding(horizontal = 24.dp)
            .semantics { contentDescription = label }
            .clickable(role = Role.Button, enabled = !busy, onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, color = onColorFor(fill), style = MaterialTheme.typography.labelLarge)
    }
}
