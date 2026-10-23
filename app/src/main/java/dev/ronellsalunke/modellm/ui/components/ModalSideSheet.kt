package dev.ronellsalunke.modellm.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.ronellsalunke.modellm.ui.theme.LocalAppPalette

private val SideSheetMaxWidth = 400.dp
private val SideSheetCornerRadius = 16.dp

/**
 * Modal sheet anchored to the end edge, for windows wide enough that a bottom sheet would cover
 * the content behind it. Dismisses on back, on a click outside the panel, or from [content].
 * Clicking the panel itself does not dismiss the sheet.
 */
@Composable
fun ModalSideSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false,
        ),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val panelWidth = minOf(SideSheetMaxWidth, maxWidth)
            Row(Modifier.fillMaxSize()) {
                Box(
                    Modifier.weight(1f).fillMaxHeight().clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss,
                    ).semantics { contentDescription = "Close sheet" }
                )
                Surface(
                    modifier = modifier.width(panelWidth).fillMaxHeight(),
                    color = LocalAppPalette.current.background,
                    shape = RoundedCornerShape(
                        topStart = SideSheetCornerRadius,
                        bottomStart = SideSheetCornerRadius,
                    ),
                ) {
                    content()
                }
            }
        }
    }
}
