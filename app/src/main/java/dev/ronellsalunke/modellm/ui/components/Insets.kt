package dev.ronellsalunke.modellm.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

/**
 * Bottom inset for scrollable content so the last row clears the navigation bar and keyboard.
 * Pass to contentPadding, never to a parent modifier.
 */
@Composable
fun safeDrawingBottomPadding(): Dp =
    WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding()

/**
 * Bottom inset that clears the navigation bar and cutout but not the keyboard.
 * Use it for lists inside a container that already applies [imePadding].
 */
@Composable
fun navigationBarBottomPadding(): Dp =
    WindowInsets.systemBars.union(WindowInsets.displayCutout)
        .asPaddingValues().calculateBottomPadding()

/**
 * Keeps fixed screen content clear of side system bars and cutouts.
 * Apply to a screen root column, not to a list parent.
 */
@Composable
fun Modifier.safeDrawingHorizontalPadding(): Modifier =
    windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))

/**
 * Padding for a tab screen: side system bars and the keyboard.
 * The screen's list must add [navigationBarBottomPadding] to its content padding.
 */
@Composable
fun Modifier.tabScreenPadding(): Modifier = safeDrawingHorizontalPadding().imePadding()
