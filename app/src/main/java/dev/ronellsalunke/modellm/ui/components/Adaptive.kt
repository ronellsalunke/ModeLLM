package dev.ronellsalunke.modellm.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val MediumWidthBreakpoint = 600.dp
val MinGridCellWidth = 280.dp

/** Bottom space a tab list must reserve for the navigation area. */
val LocalNavigationAreaBottomPadding = staticCompositionLocalOf { 0.dp }

/** True when the window is at least the medium width breakpoint (600 dp). */
val LocalWideLayout = staticCompositionLocalOf { false }

/** True when the detail is shown on its own; false when the list stays visible beside it. */
val LocalDetailShowsBackButton = staticCompositionLocalOf { true }

@Composable
fun navigationAreaBottomPadding(): Dp = LocalNavigationAreaBottomPadding.current
