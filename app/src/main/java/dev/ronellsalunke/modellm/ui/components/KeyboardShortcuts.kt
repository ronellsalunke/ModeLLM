package dev.ronellsalunke.modellm.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager

internal fun KeyEvent.isControlShortcut(): Boolean =
    type == KeyEventType.KeyDown && isCtrlPressed && nativeKeyEvent.repeatCount == 0

internal class CatalogShortcutActions(
    val searchFocus: FocusRequester,
    val refresh: () -> Unit,
)

internal class CatalogShortcutState(
    /**
     * Focus target shared by every tab page. It lives above the pager so restoring focus never
     * asks the pager to bring a page into view, which would interrupt a swipe.
     */
    val screenFocus: FocusRequester = FocusRequester(),
) {
    var actions: CatalogShortcutActions? = null

    fun handle(event: KeyEvent): Boolean {
        val current = actions
        if (current == null || !event.isControlShortcut()) return false
        return when (event.key) {
            Key.F -> { current.searchFocus.requestFocus(); true }
            Key.R -> { current.refresh(); true }
            else -> false
        }
    }
}

internal val LocalCatalogShortcuts = staticCompositionLocalOf<CatalogShortcutState?> { null }

/**
 * False for a pager page that stays composed while another page is on screen. Such a page must
 * not claim the catalog shortcuts, which belong to the visible page.
 */
internal val LocalTabPageActive = staticCompositionLocalOf { true }

@Composable
internal fun dismissCatalogFocus(): () -> Unit {
    val focusManager = LocalFocusManager.current
    val shortcuts = LocalCatalogShortcuts.current
    return {
        focusManager.clearFocus()
        shortcuts?.screenFocus?.requestFocus()
    }
}

internal fun Modifier.dismissSearchOnEscape(onDismiss: () -> Unit): Modifier =
    onPreviewKeyEvent { event ->
        if (event.key == Key.Escape && event.type == KeyEventType.KeyDown) {
            onDismiss()
            true
        } else false
    }

@Composable
fun Modifier.catalogShortcuts(
    searchFocusRequester: FocusRequester,
    onRefresh: () -> Unit,
): Modifier {
    val shortcuts = LocalCatalogShortcuts.current
    val active = LocalTabPageActive.current
    val refresh by rememberUpdatedState(onRefresh)
    val actions = remember(searchFocusRequester) {
        CatalogShortcutActions(searchFocusRequester) { refresh() }
    }
    DisposableEffect(shortcuts, actions, active) {
        if (active) shortcuts?.actions = actions
        onDispose {
            if (shortcuts?.actions === actions) shortcuts.actions = null
        }
    }
    LaunchedEffect(active) {
        // Pull focus out of the page that just left the screen without focusing a node inside the
        // pager, which would scroll the pager and could reverse an in-flight swipe.
        if (active) shortcuts?.screenFocus?.requestFocus()
    }
    return onPreviewKeyEvent { event ->
        if (!active) return@onPreviewKeyEvent false
        shortcuts?.handle(event) ?: false
    }
}
