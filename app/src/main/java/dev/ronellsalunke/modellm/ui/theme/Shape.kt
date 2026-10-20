package dev.ronellsalunke.modellm.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Radii sampled from models.dev: 2px icon tiles, 4px controls, 6px menu rows, 8px dialogs. */
val ModelsDevShapes =
    Shapes(
        extraSmall = RoundedCornerShape(2.dp),
        small = RoundedCornerShape(4.dp),
        medium = RoundedCornerShape(6.dp),
        large = RoundedCornerShape(8.dp),
        extraLarge = RoundedCornerShape(12.dp),
    )

object Dimens {
    val hairline = 1.dp
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp

    val listRowMinHeight = 48.dp
    val iconSmall = 16.dp
    val iconMedium = 20.dp
    val touchTarget = 40.dp
}
