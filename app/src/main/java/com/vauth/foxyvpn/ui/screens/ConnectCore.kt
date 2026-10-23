package com.vauth.foxyvpn.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Power
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vauth.foxyvpn.data.model.ConnectionState
import com.vauth.foxyvpn.ui.theme.ConnectButtonStyle

private val PressSpring = spring<Float>(dampingRatio = 0.35f, stiffness = 700f)
private val MorphSpring = spring<Float>(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium)

/**
 * The rebuilt connect core (Bug 1): springy press feedback, smooth morphing
 * between disconnected → connecting → connected, and a lively breathing
 * pulse while connected. One shared animation core, six visual styles.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConnectCore(
    state: ConnectionState,
    style: ConnectButtonStyle = ConnectButtonStyle.Hero,
    reduceMotion: Boolean = false,
    connectColor: Color? = null,
    connectedColor: Color? = null,
    elapsedText: String? = null,
    onToggle: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val live = state == ConnectionState.CONNECTED
    val busy = state == ConnectionState.CONNECTING

    val idleColor = connectColor ?: cs.primary
    val activeColor = connectedColor ?: cs.primary
    val container by animateColorAsState(
        targetValue = when {
            live -> activeColor
            busy -> cs.tertiary
            else -> idleColor
        },
        animationSpec = if (reduceMotion) tween(120) else tween(450),
        label = "core-color",
    )
    val onContainer = container.readableOn()

    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = if (reduceMotion) tween(80) else PressSpring,
        label = "core-press",
    )
    val haptics = LocalHapticFeedback.current

    Box(
        modifier = Modifier.graphicsLayer {
            scaleX = pressScale
            scaleY = pressScale
        },
        contentAlignment = Alignment.Center,
    ) {
        // Lively breathing pulse rings while connected.
        if (live && !reduceMotion) {
            ConnectedPulseRings(color = container)
        }
        Box(
            modifier = Modifier.combinedClickable(
                interactionSource = source,
                indication = null,
                role = Role.Button,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggle()
                },
            ),
            contentAlignment = Alignment.Center,
        ) {
            when (style) {
                ConnectButtonStyle.Hero -> HeroCore(
                    live = live, busy = busy, reduceMotion = reduceMotion,
                    container = container, onContainer = onContainer,
                    elapsedText = elapsedText,
                )
                ConnectButtonStyle.Ring -> RingCore(
                    live = live, busy = busy, reduceMotion = reduceMotion,
                    container = container, onContainer = onContainer,
                )
                ConnectButtonStyle.Pill -> PillCore(
                    live = live, busy = busy, reduceMotion = reduceMotion,
                    container = container, onContainer = onContainer,
                )
                ConnectButtonStyle.Bar -> BarCore(
                    live = live, busy = busy, reduceMotion = reduceMotion,
                    container = container, onContainer = onContainer,
                )
                ConnectButtonStyle.Icon -> IconCore(
                    live = live, busy = busy, reduceMotion = reduceMotion,
                    container = container, onContainer = onContainer,
                )
                ConnectButtonStyle.Switch -> SwitchCore(
                    live = live, busy = busy, reduceMotion = reduceMotion,
                    container = container, onContainer = onContainer,
                )
            }
        }
    }
}

/** Two staggered expanding rings that breathe while connected. */
@Composable
private fun ConnectedPulseRings(color: Color) {
    val transition = rememberInfiniteTransition(label = "pulse")
    repeat(2) { index ->
        val progress by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart,
                initialStartOffset = androidx.compose.animation.core.StartOffset(index * 1100),
            ),
            label = "pulse-$index",
        )
        Canvas(Modifier.size(210.dp)) {
            drawCircle(
                color = color.copy(alpha = (1f - progress) * 0.35f),
                radius = size.minDimension / 2f * (0.72f + 0.38f * progress),
                style = Stroke(width = (10f * (1f - progress) + 2f)),
            )
        }
    }
}

private fun Color.readableOn(): Color =
    if (luminance() > 0.5f) Color(0xFF101418) else Color.White

/** Hero: big morphing blob — circle idle, soft blooming squircle when live. */
@Composable
private fun HeroCore(
    live: Boolean,
    busy: Boolean,
    reduceMotion: Boolean,
    container: Color,
    onContainer: Color,
    elapsedText: String?,
) {
    val cs = MaterialTheme.colorScheme
    val morph by animateFloatAsState(
        targetValue = if (live) 1f else 0f,
        animationSpec = if (reduceMotion) tween(150) else MorphSpring,
        label = "hero-morph",
    )
    val size: Dp by animateDpAsState(
        targetValue = if (live) 184.dp else 168.dp,
        animationSpec = if (reduceMotion) tween(150) else spring(dampingRatio = 0.6f, stiffness = 320f),
        label = "hero-size",
    )
    val corner = (50f - 26f * morph)
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(corner))
            .background(
                Brush.linearGradient(
                    listOf(container, cs.tertiary.copy(alpha = 0.85f)),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (busy) {
            ConnectingArc(color = onContainer, reduceMotion = reduceMotion, size = 64.dp)
        } else if (live && elapsedText != null) {
            Text(
                text = elapsedText,
                style = MaterialTheme.typography.headlineMediumEmphasized,
                color = onContainer,
            )
        } else {
            val iconScale by animateFloatAsState(
                targetValue = if (live) 0.8f else 1f,
                animationSpec = if (reduceMotion) tween(120) else PressSpring,
                label = "hero-icon",
            )
            Icon(
                imageVector = Icons.Filled.Power,
                contentDescription = if (live) "Disconnect" else "Connect",
                tint = onContainer,
                modifier = Modifier
                    .size(60.dp)
                    .graphicsLayer { scaleX = iconScale; scaleY = iconScale },
            )
        }
    }
}

/** Ring: circle wrapped in an animated glowing ring. */
@Composable
private fun RingCore(
    live: Boolean,
    busy: Boolean,
    reduceMotion: Boolean,
    container: Color,
    onContainer: Color,
) {
    val ringWidth by animateDpAsState(
        targetValue = if (live) 14.dp else 6.dp,
        animationSpec = if (reduceMotion) tween(120) else MorphSpring,
        label = "ring-width",
    )
    Box(contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(196.dp)) {
            drawCircle(
                color = container.copy(alpha = 0.35f),
                radius = size.minDimension / 2f,
                style = Stroke(width = ringWidth.toPx()),
            )
        }
        Box(
            modifier = Modifier
                .size(148.dp)
                .clip(CircleShape)
                .background(container),
            contentAlignment = Alignment.Center,
        ) {
            if (busy) {
                ConnectingArc(color = onContainer, reduceMotion = reduceMotion, size = 52.dp)
            } else {
                Icon(
                    imageVector = Icons.Filled.Power,
                    contentDescription = if (live) "Disconnect" else "Connect",
                    tint = onContainer,
                    modifier = Modifier.size(52.dp),
                )
            }
        }
    }
}

/** Pill: wide friendly pill with icon + label. */
@Composable
private fun PillCore(
    live: Boolean,
    busy: Boolean,
    reduceMotion: Boolean,
    container: Color,
    onContainer: Color,
) {
    val width: Dp by animateDpAsState(
        targetValue = if (live) 220.dp else 196.dp,
        animationSpec = if (reduceMotion) tween(120) else spring(dampingRatio = 0.6f, stiffness = 380f),
        label = "pill-width",
    )
    Row(
        modifier = Modifier
            .width(width)
            .height(88.dp)
            .clip(RoundedCornerShape(44.dp))
            .background(container)
            .padding(horizontal = 28.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (busy) {
            ConnectingArc(color = onContainer, reduceMotion = reduceMotion, size = 36.dp)
        } else {
            Icon(
                imageVector = Icons.Filled.Power,
                contentDescription = null,
                tint = onContainer,
                modifier = Modifier.size(36.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = when {
                busy -> "Connecting…"
                live -> "Connected"
                else -> "Connect"
            },
            style = MaterialTheme.typography.titleLargeEmphasized,
            color = onContainer,
            maxLines = 1,
        )
    }
}

/** Bar: full-width bar that fills up as it connects. */
@Composable
private fun BarCore(
    live: Boolean,
    busy: Boolean,
    reduceMotion: Boolean,
    container: Color,
    onContainer: Color,
) {
    val cs = MaterialTheme.colorScheme
    val fill by animateFloatAsState(
        targetValue = when {
            live -> 1f
            busy -> 0.55f
            else -> 0.12f
        },
        animationSpec = if (reduceMotion) tween(150) else spring(dampingRatio = 0.65f, stiffness = 220f),
        label = "bar-fill",
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(cs.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fill)
                .height(72.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(container)
                .align(Alignment.CenterStart),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Power,
                contentDescription = null,
                tint = if (fill > 0.5f) onContainer else cs.onSurfaceVariant,
                modifier = Modifier.size(30.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = when {
                    busy -> "Connecting…"
                    live -> "Connected — tap to disconnect"
                    else -> "Tap to connect"
                },
                style = MaterialTheme.typography.titleMediumEmphasized,
                color = if (fill > 0.5f) onContainer else cs.onSurfaceVariant,
            )
        }
    }
}

/** Icon: compact bouncy rounded square. */
@Composable
private fun IconCore(
    live: Boolean,
    busy: Boolean,
    reduceMotion: Boolean,
    container: Color,
    onContainer: Color,
) {
    val corner by animateDpAsState(
        targetValue = if (live) 36.dp else 28.dp,
        animationSpec = if (reduceMotion) tween(120) else MorphSpring,
        label = "icon-corner",
    )
    Box(
        modifier = Modifier
            .size(112.dp)
            .clip(RoundedCornerShape(corner))
            .background(container),
        contentAlignment = Alignment.Center,
    ) {
        if (busy) {
            ConnectingArc(color = onContainer, reduceMotion = reduceMotion, size = 44.dp)
        } else {
            Icon(
                imageVector = Icons.Filled.Power,
                contentDescription = if (live) "Disconnect" else "Connect",
                tint = onContainer,
                modifier = Modifier.size(48.dp),
            )
        }
    }
}

/** Switch: a big friendly toggle switch. */
@Composable
private fun SwitchCore(
    live: Boolean,
    busy: Boolean,
    reduceMotion: Boolean,
    container: Color,
    onContainer: Color,
) {
    val cs = MaterialTheme.colorScheme
    val track by animateColorAsState(
        targetValue = if (live || busy) container else cs.surfaceContainerHighest,
        animationSpec = if (reduceMotion) tween(120) else tween(350),
        label = "switch-track",
    )
    Box(
        modifier = Modifier
            .width(168.dp)
            .height(96.dp)
            .clip(RoundedCornerShape(48.dp))
            .background(track)
            .padding(10.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        val thumbOffset by animateDpAsState(
            targetValue = if (live) 62.dp else 0.dp,
            animationSpec = if (reduceMotion) tween(120)
            else spring(dampingRatio = 0.45f, stiffness = 500f),
            label = "switch-thumb",
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(thumbOffset))
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(if (live || busy) onContainer else cs.onSurfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (busy) {
                    ConnectingArc(
                        color = if (live || busy) container else cs.surface,
                        reduceMotion = reduceMotion,
                        size = 34.dp,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Power,
                        contentDescription = if (live) "Disconnect" else "Connect",
                        tint = if (live) container else cs.surface,
                        modifier = Modifier.size(36.dp),
                    )
                }
            }
        }
    }
}

/** Spinning arc shown while connecting. */
@Composable
private fun ConnectingArc(color: Color, reduceMotion: Boolean, size: Dp) {
    if (reduceMotion) {
        Canvas(Modifier.size(size)) {
            drawArc(
                color = color,
                startAngle = 0f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = 8f, cap = StrokeCap.Round),
            )
        }
        return
    }
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            rotation.animateTo(rotation.value + 360f, tween(1100, easing = LinearEasing))
        }
    }
    Canvas(
        Modifier
            .size(size)
            .graphicsLayer { rotationZ = rotation.value },
    ) {
        drawArc(
            color = color,
            startAngle = 0f,
            sweepAngle = 270f,
            useCenter = false,
            style = Stroke(width = 10f, cap = StrokeCap.Round),
        )
    }
}
