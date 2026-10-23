package dev.ronellsalunke.modellm.ui.navigation

import androidx.compose.animation.ContentTransform

internal fun pushedContentTransform(): ContentTransform =
    ContentTransform(pushedEnter(), pushedExit())

internal fun pushedPopContentTransform(): ContentTransform =
    ContentTransform(pushedPopEnter(), pushedPopExit())

internal fun pushedPredictivePopContentTransform(swipeEdge: Int): ContentTransform =
    ContentTransform(pushedPredictivePopEnter(swipeEdge), pushedPredictivePopExit(swipeEdge))
