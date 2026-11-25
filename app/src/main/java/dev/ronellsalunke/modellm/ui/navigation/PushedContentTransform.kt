package dev.ronellsalunke.modellm.ui.navigation

import androidx.compose.animation.ContentTransform
import androidx.navigationevent.NavigationEvent

internal fun pushedContentTransform(): ContentTransform =
    ContentTransform(pushedEnter(), pushedExit())

internal fun pushedPopContentTransform(
    swipeEdge: Int = NavigationEvent.EDGE_NONE,
): ContentTransform =
    ContentTransform(pushedPopEnter(swipeEdge), pushedPopExit(swipeEdge))
