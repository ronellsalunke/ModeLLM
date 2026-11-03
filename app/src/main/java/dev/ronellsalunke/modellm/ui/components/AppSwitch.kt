package dev.ronellsalunke.modellm.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

private val SwitchWidth = 52.dp
private val SwitchHeight = 32.dp
private val SwitchBorderWidth = 2.dp
private val UncheckedThumbSize = 20.dp
private val CheckedThumbSize = 24.dp
private val UncheckedThumbOffset = 6.dp
private val CheckedThumbOffset = 24.dp
private const val SwitchAnimationMillis = 150

private data class SwitchPalette(
    val track: Color,
    val thumb: Color,
    val border: Color,
)

/** Material switch with the app's square corner radii instead of the default pill track. */
@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val palette = switchPalette(enabled, checked)
    val thumbSize by
        animateDpAsState(
            if (checked) CheckedThumbSize else UncheckedThumbSize,
            animationSpec = tween(SwitchAnimationMillis),
            label = "AppSwitchThumbSize",
        )
    val thumbOffset by
        animateDpAsState(
            if (checked) CheckedThumbOffset else UncheckedThumbOffset,
            animationSpec = tween(SwitchAnimationMillis),
            label = "AppSwitchThumbOffset",
        )
    val thumbVerticalOffset = (SwitchHeight - thumbSize) / 2
    val shape = MaterialTheme.shapes.extraSmall
    val toggleModifier =
        if (onCheckedChange != null) {
            Modifier.minimumInteractiveComponentSize()
                .toggleable(
                    value = checked,
                    onValueChange = onCheckedChange,
                    enabled = enabled,
                    role = Role.Switch,
                )
        } else {
            Modifier
        }
    Box(
        modifier
            .then(toggleModifier)
            .wrapContentSize(Alignment.Center)
            .requiredSize(SwitchWidth, SwitchHeight)
            .clip(shape)
            .background(palette.track)
            .border(SwitchBorderWidth, palette.border, shape),
    ) {
        Box(
            Modifier.offset(x = thumbOffset, y = thumbVerticalOffset)
                .size(thumbSize)
                .background(palette.thumb, shape),
        )
    }
}

@Composable
private fun switchPalette(enabled: Boolean, checked: Boolean): SwitchPalette {
    val colors = SwitchDefaults.colors()
    return SwitchPalette(
        track =
            switchColor(
                enabled,
                checked,
                colors.checkedTrackColor,
                colors.uncheckedTrackColor,
                colors.disabledCheckedTrackColor,
                colors.disabledUncheckedTrackColor,
            ),
        thumb =
            switchColor(
                enabled,
                checked,
                colors.checkedThumbColor,
                colors.uncheckedThumbColor,
                colors.disabledCheckedThumbColor,
                colors.disabledUncheckedThumbColor,
            ),
        border =
            switchColor(
                enabled,
                checked,
                colors.checkedBorderColor,
                colors.uncheckedBorderColor,
                colors.disabledCheckedBorderColor,
                colors.disabledUncheckedBorderColor,
            ),
    )
}

private fun switchColor(
    enabled: Boolean,
    checked: Boolean,
    checkedColor: Color,
    uncheckedColor: Color,
    disabledCheckedColor: Color,
    disabledUncheckedColor: Color,
): Color =
    when {
        !enabled && checked -> disabledCheckedColor
        !enabled -> disabledUncheckedColor
        checked -> checkedColor
        else -> uncheckedColor
    }
