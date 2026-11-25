package dev.ronellsalunke.modellm.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation3.scene.SceneInfo
import androidx.navigation3.scene.SceneState
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.NavigationEventState

/**
 * Associates an active predictive back gesture's swipe edge with the scene being popped so
 * NavDisplay's pop transition handoff finishes exiting in the same direction as the gesture.
 */
internal class PopSwipeEdgeState {
    private var pendingSceneKey: Any? = null
    private var pendingEdge: Int = NavigationEvent.EDGE_NONE
    private var poppedSceneKey: Any? = null
    private var poppedEdge: Int = NavigationEvent.EDGE_NONE

    fun syncCurrentScene(sceneKey: Any, activeSwipeEdge: Int) {
        if (poppedSceneKey == sceneKey) {
            poppedSceneKey = null
            poppedEdge = NavigationEvent.EDGE_NONE
        }
        if (activeSwipeEdge != NavigationEvent.EDGE_NONE) {
            pendingSceneKey = sceneKey
            pendingEdge = activeSwipeEdge
        }
    }

    fun onGestureCompleted(sceneKey: Any) {
        if (pendingSceneKey == sceneKey && pendingEdge != NavigationEvent.EDGE_NONE) {
            poppedSceneKey = sceneKey
            poppedEdge = pendingEdge
        } else {
            poppedSceneKey = null
            poppedEdge = NavigationEvent.EDGE_NONE
        }
        pendingSceneKey = null
        pendingEdge = NavigationEvent.EDGE_NONE
    }

    fun onGestureCancelled() {
        pendingSceneKey = null
        pendingEdge = NavigationEvent.EDGE_NONE
    }

    fun edgeFor(sceneKey: Any): Int =
        when (sceneKey) {
            pendingSceneKey -> pendingEdge
            poppedSceneKey -> poppedEdge
            else -> NavigationEvent.EDGE_NONE
        }
}

@Composable
internal fun <T : Any> rememberScenePopSwipeEdge(
    sceneState: SceneState<T>,
    navEventState: NavigationEventState<SceneInfo<T>>,
    onBack: () -> Unit,
): PopSwipeEdgeState {
    val state = remember(navEventState) { PopSwipeEdgeState() }
    val activeEdge by remember(navEventState) {
        derivedStateOf {
            val transitionState = navEventState.transitionState
            if (transitionState is NavigationEventTransitionState.InProgress) {
                transitionState.latestEvent.swipeEdge
            } else {
                NavigationEvent.EDGE_NONE
            }
        }
    }
    state.syncCurrentScene(sceneState.currentScene.key, activeEdge)
    NavigationBackHandler(
        state = navEventState,
        isBackEnabled = sceneState.currentScene.previousEntries.isNotEmpty(),
        onBackCancelled = state::onGestureCancelled,
        onBackCompleted = {
            state.onGestureCompleted(sceneState.currentScene.key)
            repeat(sceneState.entries.size - sceneState.currentScene.previousEntries.size) {
                onBack()
            }
        },
    )
    return state
}
