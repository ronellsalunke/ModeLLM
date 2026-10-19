package dev.ronellsalunke.modellm.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SingleChoiceButtonGroup(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactions = remember(labels) { labels.map { MutableInteractionSource() } }
    ButtonGroup(
        overflowIndicator = { ButtonGroupDefaults.OverflowIndicator(it) },
        modifier = modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        labels.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            customItem(
                buttonGroupContent = {
                    ToggleButton(
                        checked = selected,
                        onCheckedChange = { checked -> if (checked) onSelect(index) },
                        shapes = when (index) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            labels.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        },
                        interactionSource = interactions[index],
                        modifier = Modifier
                            .weight(1f)
                            .animateWidth(interactions[index])
                            .semantics {
                                role = Role.RadioButton
                                this.selected = selected
                            },
                    ) { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                },
                menuContent = { menuState ->
                    SelectableDropdownMenuItem(
                        selected = selected,
                        onClick = { onSelect(index); menuState.dismiss() },
                        text = { Text(label) },
                        shapes = MenuDefaults.itemShapes(),
                    )
                },
            )
        }
    }
}
