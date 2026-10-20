package dev.ronellsalunke.modellm.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavBackStackEntry
import androidx.navigationevent.NavigationEvent

private const val PushedMotionDurationMillis = 300
private val PushedOffsetSpec =
    tween<IntOffset>(durationMillis = PushedMotionDurationMillis, easing = FastOutSlowInEasing)
private val PushedFadeSpec =
    tween<Float>(durationMillis = PushedMotionDurationMillis, easing = FastOutSlowInEasing)

private const val EnterOffsetFraction = 1f / 4f
private const val ExitOffsetFraction = -1f / 8f
private const val PopEnterOffsetFraction = -1f / 4f

private fun slideInFrom(fraction: Float): EnterTransition =
    slideInHorizontally(PushedOffsetSpec) { (it * fraction).toInt() } + fadeIn(PushedFadeSpec)

private fun slideOutTo(fraction: Float): ExitTransition =
    slideOutHorizontally(PushedOffsetSpec) { (it * fraction).toInt() } + fadeOut(PushedFadeSpec)

private fun swipeDirection(swipeEdge: Int): Float =
    if (swipeEdge == NavigationEvent.EDGE_RIGHT) -1f else 1f

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.pushedEnter(): EnterTransition =
    slideInFrom(EnterOffsetFraction)

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.pushedExit(): ExitTransition =
    slideOutTo(ExitOffsetFraction)

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.pushedPopEnter(): EnterTransition =
    slideInFrom(PopEnterOffsetFraction)

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.pushedPopExit(): ExitTransition =
    slideOutHorizontally(PushedOffsetSpec) { it }

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.pushedPredictivePopEnter(
    swipeEdge: Int,
): EnterTransition {
    val direction = swipeDirection(swipeEdge)
    return slideInFrom(PopEnterOffsetFraction * direction)
}

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.pushedPredictivePopExit(
    swipeEdge: Int,
): ExitTransition {
    val direction = swipeDirection(swipeEdge)
    return slideOutHorizontally(PushedOffsetSpec) { (it * direction).toInt() }
}
