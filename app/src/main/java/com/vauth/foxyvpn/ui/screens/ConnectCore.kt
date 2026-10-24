package com.vauth.foxyvpn.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Power
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import com.vauth.foxyvpn.data.model.ConnectionState
import com.vauth.foxyvpn.ui.theme.ConnectButtonStyle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay

// ─── Motion budget (1:1 from ZedSecure ui/theme/MotionBudget.kt) ─────────────

enum class MotionBudget { Full, Throttled, Paused }

val LocalMotionBudget = compositionLocalOf { MotionBudget.Full }

// ─── Portal clock (1:1 from ZedSecure PortalHome.kt:312-326) ─────────────────
// Runs ONLY when `running` is true — idle screens do zero per-frame work.

@Composable
fun rememberPortalClock(running: Boolean): State<Float> {
    val time = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        val base = time.floatValue
        var start = -1L
        while (true) {
            withInfiniteAnimationFrameNanos { now ->
                if (start < 0L) start = now
                time.floatValue = base + (now - start) / 1_000_000_000f
            }
        }
    }
    return time
}

// ─── Shape constants (1:1 from ZedSecure PortalHome.kt:129-141) ──────────────

val SearchShapes: List<RoundedPolygon> = listOf(
    MaterialShapes.SoftBurst,
    MaterialShapes.Cookie9Sided,
    MaterialShapes.Pentagon,
    MaterialShapes.Pill,
    MaterialShapes.Sunny,
    MaterialShapes.Cookie4Sided,
    MaterialShapes.Oval,
)

const val SEARCH_STEP_SECONDS = 0.72f

private const val TUNNEL_RINGS = 6

/** 1:1 from ZedSecure PixelHome.kt */
private val DoorSpring = spring<Dp>(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)

// ─── Scoped elapsed-seconds timer ────────────────────────────────────────────
// Lives inside the connect core: only the elapsed Text recomposes each tick,
// never the whole home screen (this was a major v1.2 lag source).

@Composable
private fun rememberElapsedSeconds(state: ConnectionState): Int {
    var elapsed by remember { mutableIntStateOf(0) }
    LaunchedEffect(state) {
        if (state == ConnectionState.CONNECTED) {
            val start = System.currentTimeMillis()
            while (true) {
                delay(1_000L)
                elapsed = ((System.currentTimeMillis() - start) / 1_000L).toInt()
            }
        } else {
            elapsed = 0
        }
    }
    return elapsed
}

private const val TABULAR = "tnum"

private fun tabularDigits(text: String): AnnotatedString = buildAnnotatedString {
    var start = 0
    while (start < text.length) {
        val digits = text[start].isDigit()
        var end = start + 1
        while (end < text.length && text[end].isDigit() == digits) end++
        val from = length
        append(text, start, end)
        if (digits) addStyle(SpanStyle(fontFeatureSettings = TABULAR), from, length)
        start = end
    }
}

// ─── Entry point ─────────────────────────────────────────────────────────────

/**
 * ZedSecure-faithful connect core (v1.3): every animation parameter mirrors
 * ZedSecure's implementation 1:1 — same springs, same tweens, same easings,
 * same shapes, same sizes, same colors, same motion budget gating.
 * Idle screens perform ZERO per-frame work.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class)
@Composable
fun ConnectCore(
    state: ConnectionState,
    style: ConnectButtonStyle = ConnectButtonStyle.Hero,
    reduceMotion: Boolean = false,
    hapticEnabled: Boolean = true,
    connectColor: Color? = null,
    connectedColor: Color? = null,
    onToggle: () -> Unit,
) {
    val live = state == ConnectionState.CONNECTED
    val searching = state == ConnectionState.CONNECTING
    val budget = LocalMotionBudget.current
    // 1:1 from ZedSecure PixelHome.kt
    val animate = !reduceMotion && when (budget) {
        MotionBudget.Full -> true
        MotionBudget.Throttled -> searching
        MotionBudget.Paused -> false
    }
    val clock = rememberPortalClock(animate && (live || searching))
    val haptics = LocalHapticFeedback.current

    // 1:1 from ZedSecure PixelHome.kt: haptic + one-shot celebration on connect.
    var celebrate by remember { mutableIntStateOf(0) }
    var previous by remember { mutableStateOf(state) }
    LaunchedEffect(state) {
        if (state == ConnectionState.CONNECTED && previous == ConnectionState.CONNECTING) {
            if (hapticEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            if (!reduceMotion) celebrate++
        }
        previous = state
    }

    when (style) {
        ConnectButtonStyle.Hero -> BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            // 1:1 from ZedSecure PixelHome.kt: full = minOf(maxWidth*0.52, maxHeight*0.46)
            // coerced to 116.dp..208.dp. (No details panel in our app, so no `small` size.)
            val full = (maxWidth * 0.52f).coerceIn(116.dp, 208.dp)
            PixelDoor(
                state = state,
                doorSize = full,
                clock = clock,
                animate = animate,
                celebrate = celebrate,
                reduceMotion = reduceMotion,
                onTap = {
                    if (hapticEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggle()
                },
            )
        }
        ConnectButtonStyle.Ring -> RingConnect(state, onToggle, connectColor, connectedColor)
        ConnectButtonStyle.Bar -> BarConnect(state, onToggle, connectColor, connectedColor)
        ConnectButtonStyle.Icon -> IconConnect(state, onToggle, connectColor, connectedColor)
        ConnectButtonStyle.Switch -> SwitchConnect(state, onToggle, connectColor, connectedColor)
        ConnectButtonStyle.Pill -> PillConnect(state, onToggle, connectColor, connectedColor)
    }
}

// ─── Pixel door — 1:1 from ZedSecure PixelHome.kt PixelDoor ──────────────────

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class)
@Composable
private fun PixelDoor(
    state: ConnectionState,
    doorSize: Dp,
    clock: State<Float>,
    animate: Boolean,
    celebrate: Int,
    reduceMotion: Boolean,
    onTap: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val searching = state == ConnectionState.CONNECTING
    val live = state == ConnectionState.CONNECTED

    // 1:1: shape step timer only while searching.
    var step by remember { mutableIntStateOf(0) }
    LaunchedEffect(searching, animate) {
        if (!searching || !animate) return@LaunchedEffect
        snapshotFlow { (clock.value / SEARCH_STEP_SECONDS).toInt() }.collect { step = it }
    }

    // 1:1 shape targets.
    val target: RoundedPolygon = when {
        live -> MaterialShapes.SoftBurst
        searching -> SearchShapes[step.mod(SearchShapes.size)]
        else -> MaterialShapes.Cookie12Sided
    }
    var from by remember { mutableStateOf(MaterialShapes.Cookie12Sided) }
    var to by remember { mutableStateOf(MaterialShapes.Cookie12Sided) }
    var progress by remember { mutableFloatStateOf(1f) }
    LaunchedEffect(target) {
        if (target === to) return@LaunchedEffect
        Snapshot.withMutableSnapshot {
            from = if (progress >= 0.5f) to else from
            to = target
            progress = 0f
        }
        // 1:1: spring(dampingRatio = 0.6f, stiffness = 260f)
        animate(0f, 1f, animationSpec = spring(dampingRatio = 0.6f, stiffness = 260f)) { value, _ ->
            progress = value
        }
    }
    val morph = remember(from, to) { Morph(from, to) }

    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    // 1:1: 0.9f pressed, fastSpatialSpec
    val squash by animateFloatAsState(
        if (pressed) 0.9f else 1f,
        MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "pixel-door-squash",
    )
    val effects = MaterialTheme.motionScheme.defaultEffectsSpec<Color>()
    val start by animateColorAsState(cs.primary, effects, label = "pixel-door-a")
    // 1:1: tertiary while live or searching, else primary.
    val end by animateColorAsState(
        if (live || searching) cs.tertiary else cs.primary,
        effects,
        label = "pixel-door-b",
    )
    val elapsed = rememberElapsedSeconds(state)
    val work = remember { Path() }
    val density = LocalDensity.current

    Box(
        Modifier
            .size(doorSize)
            .graphicsLayer {
                scaleX = squash
                scaleY = squash
            }
            .combinedClickable(
                interactionSource = source,
                indication = null,
                role = Role.Button,
                onClick = onTap,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.matchParentSize()) {
            work.rewind()
            morph.toPath(progress.coerceIn(0f, 1f), work)
            work.transform(Matrix().apply { scale(size.width, size.height) })
            val bounds = work.getBounds()
            work.translate(center - bounds.center)
            // 1:1 rotation speeds.
            val turn = when {
                searching && animate -> clock.value * 150f
                live && animate -> clock.value * 9f
                else -> 0f
            }
            // 1:1 brushes: sweep while searching, linear otherwise.
            val brush = if (searching) {
                Brush.sweepGradient(listOf(start, end, start), center)
            } else {
                Brush.linearGradient(listOf(start, end), Offset.Zero, Offset(size.width, size.height))
            }
            rotate(turn) { drawPath(work, brush) }
        }
        // 1:1 center content: fadeIn(260) togetherWith fadeOut(200).
        AnimatedContent(
            targetState = live,
            transitionSpec = { fadeIn(tween(260)) togetherWith fadeOut(tween(200)) },
            label = "pixel-door-center",
        ) { open ->
            if (open) {
                Text(
                    text = tabularDigits(formatElapsed(elapsed)),
                    style = MaterialTheme.typography.headlineSmallEmphasized.copy(
                        // 1:1: fontSize = doorSize * 0.14f
                        fontSize = with(density) { (doorSize * 0.14f).toSp() },
                    ),
                    color = cs.onPrimary,
                    maxLines = 1,
                    softWrap = false,
                )
            } else {
                Icon(
                    Icons.Filled.Power,
                    contentDescription = null,
                    tint = cs.onPrimary,
                    // 1:1: icon size = doorSize * 0.32f
                    modifier = Modifier.size(doorSize * 0.32f),
                )
            }
        }
        CelebrationBurst(celebrate, Modifier.matchParentSize())
    }
}

// ─── Celebration burst — 1:1 from ZedSecure PixelHome.kt ─────────────────────

private const val BURST_BITS = 16

@Composable
private fun CelebrationBurst(trigger: Int, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val colors = listOf(cs.primary, cs.tertiary, cs.secondary, cs.tertiaryContainer, cs.secondaryContainer)
    val progress = remember { Animatable(1f) }
    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        progress.snapTo(0f)
        // 1:1: tween(950, FastOutSlowInEasing)
        progress.animateTo(1f, tween(950, easing = FastOutSlowInEasing))
    }
    Canvas(modifier) {
        val p = progress.value
        if (p >= 1f) return@Canvas
        val bit = 5.dp.toPx()
        val reach = size.minDimension * 0.9f
        for (i in 0 until BURST_BITS) {
            val a = i * (2f * PI.toFloat() / BURST_BITS) + 0.35f
            val d = size.minDimension * 0.42f + reach * p
            val pos = Offset(center.x + cos(a) * d, center.y + sin(a) * d)
            val color = colors[i % colors.size].copy(alpha = (1f - p).coerceIn(0f, 1f))
            val s = bit * (1f - p * 0.35f)
            if (i % 2 == 0) {
                drawCircle(color, s, pos)
            } else {
                drawRoundRect(color, pos - Offset(s, s), Size(s * 2f, s * 2f), CornerRadius(s * 0.6f))
            }
        }
    }
}

// ─── Alt connect controls — 1:1 from ZedSecure ConnectControls.kt ────────────

@Composable
private fun containerFor(
    state: ConnectionState,
    custom: Color?,
    customActive: Color?,
): Color {
    val idle = custom ?: MaterialTheme.colorScheme.primary
    val active = customActive ?: MaterialTheme.colorScheme.secondaryContainer
    // 1:1: no Error state in our model; isActive == CONNECTED.
    val target = if (state == ConnectionState.CONNECTED) active else idle
    // 1:1: default animation spec.
    val animated by animateColorAsState(target, label = "alt-connect-container")
    return animated
}

@Composable
private fun contentFor(container: Color): Color =
    // 1:1: luminance > 0.5 -> 0xFF10131A else White.
    if (container.luminance() > 0.5f) Color(0xFF10131A) else Color.White

private fun label(state: ConnectionState): String = when (state) {
    ConnectionState.CONNECTING -> "Cancel"
    ConnectionState.CONNECTED -> "Disconnect"
    else -> "Connect"
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RingConnect(
    state: ConnectionState,
    onToggle: () -> Unit,
    custom: Color?,
    customActive: Color?,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    // 1:1: 0.94f pressed, default spec.
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, label = "ring-scale")
    // 1:1: 8.dp active / 3.dp idle, default spec.
    val ring by animateDpAsState(if (state == ConnectionState.CONNECTED) 8.dp else 3.dp, label = "ring-width")
    val container = containerFor(state, custom, customActive)
    val content = contentFor(container)
    val transitioning = state == ConnectionState.CONNECTING
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Surface(
            onClick = onToggle,
            shape = CircleShape,
            color = container,
            contentColor = content,
            interactionSource = interaction,
            modifier = Modifier
                .size(132.dp)
                .scale(scale)
                .border(ring, content.copy(alpha = 0.35f), CircleShape),
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (transitioning) {
                    ContainedLoadingIndicator(Modifier.size(38.dp))
                } else {
                    Icon(
                        if (state == ConnectionState.CONNECTED) Icons.Filled.Close else Icons.Filled.Bolt,
                        contentDescription = label(state),
                        modifier = Modifier.size(46.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BarConnect(
    state: ConnectionState,
    onToggle: () -> Unit,
    custom: Color?,
    customActive: Color?,
) {
    val container = containerFor(state, custom, customActive)
    val content = contentFor(container)
    val transitioning = state == ConnectionState.CONNECTING
    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(14.dp),
        color = container,
        contentColor = content,
        modifier = Modifier.fillMaxWidth().height(54.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (transitioning) {
                ContainedLoadingIndicator(Modifier.size(20.dp))
            } else {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(content.copy(alpha = if (state == ConnectionState.CONNECTED) 1f else 0.55f)),
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                label(state),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun IconConnect(
    state: ConnectionState,
    onToggle: () -> Unit,
    custom: Color?,
    customActive: Color?,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    // 1:1: 34.dp pressed / 24.dp idle, fastSpatialSpec.
    val radius by animateDpAsState(
        if (pressed) 34.dp else 24.dp,
        MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "icon-radius",
    )
    val container = containerFor(state, custom, customActive)
    val content = contentFor(container)
    val transitioning = state == ConnectionState.CONNECTING
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Surface(
            onClick = onToggle,
            shape = RoundedCornerShape(radius),
            color = container,
            contentColor = content,
            interactionSource = interaction,
            modifier = Modifier.size(76.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (transitioning) {
                    ContainedLoadingIndicator(Modifier.size(26.dp))
                } else {
                    Icon(
                        if (state == ConnectionState.CONNECTED) Icons.Filled.Close else Icons.Filled.Bolt,
                        contentDescription = label(state),
                        modifier = Modifier.size(30.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SwitchConnect(
    state: ConnectionState,
    onToggle: () -> Unit,
    custom: Color?,
    customActive: Color?,
) {
    val container = containerFor(state, custom, customActive)
    val content = contentFor(container)
    val active = state == ConnectionState.CONNECTED
    val transitioning = state == ConnectionState.CONNECTING
    // 1:1: align 1f / -1f, default spec.
    val align by animateFloatAsState(if (active) 1f else -1f, label = "switch-align")
    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(34.dp),
        color = container,
        contentColor = content,
        modifier = Modifier.fillMaxWidth().height(68.dp),
    ) {
        Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.CenterStart) {
            Box(
                Modifier.fillMaxWidth(),
                contentAlignment = androidx.compose.ui.BiasAlignment(align, 0f),
            ) {
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(content.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (transitioning) {
                        ContainedLoadingIndicator(Modifier.size(22.dp))
                    } else {
                        Icon(
                            if (active) Icons.Filled.Close else Icons.Filled.Bolt,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                // 1:1: fadeIn togetherWith fadeOut.
                AnimatedContent(
                    targetState = label(state),
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "switch-label",
                ) { text ->
                    Text(
                        text,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

/**
 * Pill style in ZedSecure's motion language (ZedSecure has no Pill control —
 * press 0.94, default specs, fading label, expressive loading indicator).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PillConnect(
    state: ConnectionState,
    onToggle: () -> Unit,
    custom: Color?,
    customActive: Color?,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, label = "pill-scale")
    val container = containerFor(state, custom, customActive)
    val content = contentFor(container)
    val transitioning = state == ConnectionState.CONNECTING
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Surface(
            onClick = onToggle,
            shape = RoundedCornerShape(50),
            color = container,
            contentColor = content,
            interactionSource = interaction,
            modifier = Modifier.scale(scale),
        ) {
            Row(
                Modifier.padding(horizontal = 28.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                if (transitioning) {
                    ContainedLoadingIndicator(Modifier.size(26.dp))
                } else {
                    Icon(
                        if (state == ConnectionState.CONNECTED) Icons.Filled.Close else Icons.Filled.Bolt,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                AnimatedContent(
                    targetState = label(state),
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "pill-label",
                ) { text ->
                    Text(
                        text,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
