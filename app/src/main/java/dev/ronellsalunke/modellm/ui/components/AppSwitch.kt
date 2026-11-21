package dev.ronellsalunke.modellm.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.ronellsalunke.modellm.ui.theme.Dimens
import dev.ronellsalunke.modellm.ui.theme.LocalAppPalette
import dev.ronellsalunke.modellm.ui.theme.LocalMaterialYouEnabled

private val SwitchWidth = 40.dp
private val SwitchHeight = 24.dp
private val SwitchBorderWidth = Dimens.hairline
private val ThumbInset = 3.dp
private val ThumbSize = 18.dp
private val PressedThumbWidth = 21.dp
private val ThumbElevation = 1.dp
private val ThumbEdgeWidth = 0.5.dp
private val TrackShape = RoundedCornerShape(5.dp)
private val ThumbShape = RoundedCornerShape(2.dp)
private const val SwitchAnimationMillis = 150
private const val DisabledAlpha = 0.5f
private const val OutlineStrength = 0.85f
private const val DarkLuminanceThreshold = 0.179f
private const val ThumbEdgeAlpha = 0.10f
private const val ThumbShadowAlpha = 0.35f

private data class SwitchPalette(
    val checkedTrack: Color,
    val checkedThumb: Color,
    val uncheckedTrack: Color,
    val uncheckedBorder: Color,
    val uncheckedThumb: Color,
    val thumbEdge: Color,
    val thumbShadow: Color,
)

private data class AnimatedSwitchColors(
    val track: Color,
    val border: Color,
    val thumb: Color,
)

/**
 * Compact square switch with concentric corners, press-widening thumb feedback, and an accessible
 * 48 dp touch target. Pass `onCheckedChange = null` with a shared [interactionSource] when the
 * surrounding row handles toggling.
 */
@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val palette = switchPalette()
    val colors = animateSwitchColors(palette, checked)

    val thumbWidthTarget = if (pressed && enabled) PressedThumbWidth else ThumbSize
    val thumbXTarget =
        if (checked) SwitchWidth - ThumbInset - thumbWidthTarget else ThumbInset
    val thumbWidth by
        animateDpAsState(
            targetValue = thumbWidthTarget,
            animationSpec = tween(SwitchAnimationMillis),
            label = "AppSwitchThumbWidth",
        )
    val thumbX by
        animateDpAsState(
            targetValue = thumbXTarget,
            animationSpec = tween(SwitchAnimationMillis),
            label = "AppSwitchThumbX",
        )

    val toggleModifier = switchToggleModifier(checked, onCheckedChange, enabled, source)
    val thumbEdge = switchThumbEdgeModifier(palette)

    Box(
        modifier
            .then(toggleModifier)
            .wrapContentSize(Alignment.Center)
            .requiredSize(SwitchWidth, SwitchHeight)
            .alpha(if (enabled) 1f else DisabledAlpha)
            .background(colors.track, TrackShape)
            .border(SwitchBorderWidth, colors.border, TrackShape),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier.offset { IntOffset(thumbX.roundToPx(), 0) }
                .size(thumbWidth, ThumbSize)
                .then(thumbEdge)
                .background(colors.thumb, ThumbShape),
        )
    }
}

private fun switchToggleModifier(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    enabled: Boolean,
    interactionSource: MutableInteractionSource,
): Modifier {
    if (onCheckedChange == null) return Modifier
    return Modifier.minimumInteractiveComponentSize()
        .toggleable(
            value = checked,
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            role = Role.Switch,
            onValueChange = onCheckedChange,
        )
        .then(if (enabled) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier)
}

private fun switchThumbEdgeModifier(palette: SwitchPalette): Modifier {
    if (palette.thumbEdge == Color.Transparent) return Modifier
    return Modifier.shadow(
            elevation = ThumbElevation,
            shape = ThumbShape,
            clip = false,
            ambientColor = palette.thumbShadow,
            spotColor = palette.thumbShadow,
        )
        .border(ThumbEdgeWidth, palette.thumbEdge, ThumbShape)
}

@Composable
private fun animateSwitchColors(palette: SwitchPalette, checked: Boolean): AnimatedSwitchColors {
    val spec = tween<Color>(SwitchAnimationMillis)
    val track by
        animateColorAsState(
            targetValue = if (checked) palette.checkedTrack else palette.uncheckedTrack,
            animationSpec = spec,
            label = "AppSwitchTrack",
        )
    val border by
        animateColorAsState(
            targetValue = if (checked) palette.checkedTrack else palette.uncheckedBorder,
            animationSpec = spec,
            label = "AppSwitchBorder",
        )
    val thumb by
        animateColorAsState(
            targetValue = if (checked) palette.checkedThumb else palette.uncheckedThumb,
            animationSpec = spec,
            label = "AppSwitchThumb",
        )
    return AnimatedSwitchColors(track = track, border = border, thumb = thumb)
}

@Composable
private fun switchPalette(): SwitchPalette {
    val palette = LocalAppPalette.current
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() <= DarkLuminanceThreshold
    val uncheckedBorder =
        if (LocalMaterialYouEnabled.current) {
            scheme.outline
        } else {
            lerp(palette.background, palette.textTertiary, OutlineStrength)
        }
    val uncheckedThumb =
        if (isDark) {
            palette.textSecondary
        } else {
            scheme.surfaceContainerLowest
        }
    val thumbEdge = if (isDark) Color.Transparent else Color.Black.copy(alpha = ThumbEdgeAlpha)
    return SwitchPalette(
        checkedTrack = scheme.primary,
        checkedThumb = scheme.onPrimary,
        uncheckedTrack = scheme.surfaceContainerHighest,
        uncheckedBorder = uncheckedBorder,
        uncheckedThumb = uncheckedThumb,
        thumbEdge = thumbEdge,
        thumbShadow = Color.Black.copy(alpha = ThumbShadowAlpha),
    )
}
