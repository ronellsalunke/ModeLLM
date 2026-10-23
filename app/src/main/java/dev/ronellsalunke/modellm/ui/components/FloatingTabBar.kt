package dev.ronellsalunke.modellm.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import dev.ronellsalunke.modellm.ui.theme.Dimens
import dev.ronellsalunke.modellm.ui.theme.LocalAppPalette
import dev.ronellsalunke.modellm.ui.theme.ModelsDevShapes

/** Bottom space the lists must leave so their last row can scroll above the floating bar. */
val FloatingTabBarClearance =
    FloatingToolbarDefaults.ContainerSize + FloatingToolbarDefaults.ScreenOffset + Dimens.sm

data class FloatingTabItem(val label: String, val icon: ImageVector)

/**
 * Floating pill navigation bar built on Material 3's [HorizontalFloatingToolbar] and
 * [ToggleButton]. The selected item shows an icon and uses the toolbar's default container styling.
 */
@Composable
fun FloatingTabBar(
    items: List<FloatingTabItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    HorizontalFloatingToolbar(
        expanded = true,
        modifier =
            modifier
                .windowInsetsPadding(
                    // Ignore the keyboard so the bar stays behind it while you type.
                    WindowInsets.safeDrawing
                        .exclude(WindowInsets.ime)
                        .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
                )
                .selectableGroup(),
    ) {
        items.forEachIndexed { index, item ->
            val selected = index == selectedIndex
            ToggleButton(
                checked = selected,
                onCheckedChange = { onSelect(index) },
                modifier =
                    Modifier.pointerHoverIcon(PointerIcon.Hand).semantics {
                        role = Role.Tab
                        this.selected = selected
                    },
                icon =
                    if (selected) {
                        { Icon(imageVector = item.icon, contentDescription = null) }
                    } else {
                        null
                    },
                // ModelsDevShapes.medium is 6.dp, so keep the checked pill fully round.
                shapes =
                    ToggleButtonDefaults.shapesFor(ToggleButtonDefaults.size)
                        .copy(checkedShape = ToggleButtonDefaults.shape),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloatingTabRail(
    items: List<FloatingTabItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Start +
                            WindowInsetsSides.Top +
                            WindowInsetsSides.Bottom,
                    )
                )
                .imePadding(),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.padding(Dimens.sm),
            shape = ModelsDevShapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 3.dp,
            border = BorderStroke(1.dp, LocalAppPalette.current.border),
        ) {
            Column(
                Modifier.selectableGroup().padding(Dimens.sm)
                    .verticalScroll(rememberScrollState()),
            ) {
                items.forEachIndexed { index, item ->
                    val selected = index == selectedIndex
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                            TooltipAnchorPosition.Above,
                        ),
                        tooltip = { PlainTooltip { Text(item.label) } },
                        state = rememberTooltipState(),
                    ) {
                        IconToggleButton(
                            checked = selected,
                            onCheckedChange = { onSelect(index) },
                            modifier = Modifier.pointerHoverIcon(PointerIcon.Hand).semantics {
                                role = Role.Tab
                                this.selected = selected
                            },
                        ) {
                            Icon(item.icon, contentDescription = item.label)
                        }
                    }
                }
            }
        }
    }
}
