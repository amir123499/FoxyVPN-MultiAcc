package com.vauth.foxyvpn.ui.motion

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.graphics.TransformOrigin
import com.vauth.foxyvpn.ui.theme.ScreenTransition

/**
 * The four animations a navigation destination needs.
 *
 * Going forward uses [enter] for the new screen and [exit] for the one
 * underneath. Going back uses [popEnter] for the screen being revealed and
 * [popExit] for the one leaving. Keeping the two directions separate is what
 * makes back navigation (and the predictive-back gesture) look like the
 * reverse of the forward move instead of a second forward move.
 */
class ScreenMotion(
    val enter: EnterTransition,
    val exit: ExitTransition,
    val popEnter: EnterTransition,
    val popExit: ExitTransition,
)

// Material 3 emphasized curves: entering content decelerates into place,
// leaving content accelerates away.
private val Decelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
private val Accelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

private const val ENTER_MS = 380
private const val EXIT_MS = 220

private fun <T> decel(ms: Int = ENTER_MS, delay: Int = 0) = tween<T>(ms, delay, Decelerate)

private fun <T> accel(ms: Int = EXIT_MS) = tween<T>(ms, 0, Accelerate)

/**
 * Builds the animations for [style].
 *
 * @param reduceMotion replaces every style with a short cross-fade.
 * @param rtl mirrors horizontal movement so "forward" follows the reading direction.
 */
fun screenMotion(style: ScreenTransition, reduceMotion: Boolean, rtl: Boolean): ScreenMotion {
    if (reduceMotion) {
        val fadeInShort = fadeIn(tween(120))
        val fadeOutShort = fadeOut(tween(90))
        return ScreenMotion(fadeInShort, fadeOutShort, fadeInShort, fadeOutShort)
    }
    val s = if (rtl) -1 else 1
    return when (style) {
        ScreenTransition.Fade -> {
            // Fade-through: the old screen leaves first, then the new one arrives.
            val arrive = fadeIn(tween(220, 80, LinearOutSlowInEasing))
            val leave = fadeOut(tween(100, 0, FastOutLinearInEasing))
            ScreenMotion(arrive, leave, arrive, leave)
        }

        ScreenTransition.Slide -> ScreenMotion(
            enter = slideInHorizontally(decel()) { s * it / 6 } + fadeIn(decel(240, 70)),
            exit = slideOutHorizontally(accel()) { -s * it / 6 } + fadeOut(accel(120)),
            popEnter = slideInHorizontally(decel()) { -s * it / 6 } + fadeIn(decel(240, 70)),
            popExit = slideOutHorizontally(accel()) { s * it / 6 } + fadeOut(accel(120)),
        )

        ScreenTransition.Push -> ScreenMotion(
            enter = slideInHorizontally(decel()) { s * it } + fadeIn(decel(200)),
            exit = slideOutHorizontally(decel()) { -s * it / 4 } + fadeOut(accel(EXIT_MS)),
            popEnter = slideInHorizontally(decel()) { -s * it / 4 } + fadeIn(decel(200)),
            popExit = slideOutHorizontally(accel(320)) { s * it },
        )

        ScreenTransition.Depth -> ScreenMotion(
            enter = scaleIn(decel(), 0.85f) + fadeIn(decel(240, 60)),
            exit = scaleOut(accel(), 1.1f) + fadeOut(accel(160)),
            popEnter = scaleIn(decel(), 1.1f) + fadeIn(decel(240, 60)),
            popExit = scaleOut(accel(), 0.85f) + fadeOut(accel(160)),
        )

        ScreenTransition.Elastic -> {
            val bounce = spring<androidx.compose.ui.unit.IntOffset>(
                dampingRatio = 0.72f,
                stiffness = Spring.StiffnessMediumLow,
            )
            ScreenMotion(
                enter = slideInHorizontally(bounce) { s * it } + fadeIn(decel(180)),
                exit = slideOutHorizontally(decel(300)) { -s * it / 5 } + fadeOut(accel()),
                popEnter = slideInHorizontally(bounce) { -s * it / 5 } + fadeIn(decel(180)),
                popExit = slideOutHorizontally(accel(300)) { s * it } + fadeOut(accel(260)),
            )
        }

        ScreenTransition.Curtain -> ScreenMotion(
            enter = slideInVertically(decel()) { -it / 3 } + fadeIn(decel(260, 60)),
            exit = slideOutVertically(accel()) { it / 8 } + fadeOut(accel(180)),
            popEnter = slideInVertically(decel()) { it / 8 } + fadeIn(decel(260, 60)),
            popExit = slideOutVertically(accel()) { -it / 3 } + fadeOut(accel(180)),
        )

        ScreenTransition.Flip -> {
            // Scaling from a side edge reads as a page turning.
            val nearEdge = TransformOrigin(if (rtl) 1f else 0f, 0.5f)
            val farEdge = TransformOrigin(if (rtl) 0f else 1f, 0.5f)
            ScreenMotion(
                enter = scaleIn(decel(), 0.88f, farEdge) + fadeIn(decel(240, 60)),
                exit = scaleOut(accel(), 0.88f, nearEdge) + fadeOut(accel(180)),
                popEnter = scaleIn(decel(), 0.88f, nearEdge) + fadeIn(decel(240, 60)),
                popExit = scaleOut(accel(), 0.88f, farEdge) + fadeOut(accel(180)),
            )
        }
    }
}
