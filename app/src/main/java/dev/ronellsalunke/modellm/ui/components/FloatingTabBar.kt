package dev.ronellsalunke.modellm.ui.components

import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import dev.ronellsalunke.modellm.ui.theme.Dimens

/** Bottom space the lists must leave so their last row can scroll above the floating bar. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
val FloatingTabBarClearance =
    FloatingToolbarDefaults.ContainerSize + FloatingToolbarDefaults.ScreenOffset + Dimens.sm

data class FloatingTabItem(val label: String, val icon: ImageVector)

/**
 * Floating pill navigation bar built on Material 3's [HorizontalFloatingToolbar] and [ToggleButton].
 * The selected item shows an icon and uses the toolbar's default container styling.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FloatingTabBar(
    items: List<FloatingTabItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    HorizontalFloatingToolbar(
        expanded = true,
        modifier = modifier.selectableGroup(),
    ) {
        items.forEachIndexed { index, item ->
            val selected = index == selectedIndex
            ToggleButton(
                checked = selected,
                onCheckedChange = { if (!selected) onSelect(index) },
                modifier = Modifier.semantics {
                    role = Role.Tab
                    this.selected = selected
                },
                icon = if (selected) {
                    { Icon(imageVector = item.icon, contentDescription = null) }
                } else {
                    null
                },
                // ModelsDevShapes.medium is 6.dp, so keep the checked pill fully round.
                shapes = ToggleButtonDefaults.shapesFor(ToggleButtonDefaults.size).copy(
                    checkedShape = ToggleButtonDefaults.shape,
                ),
            ) {
                Text(
                    text = item.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
