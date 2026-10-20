package dev.ronellsalunke.modellm.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Palette sampled from models.dev. The site uses a flat, bordered surface style with one orange
 * accent and neutral greys rather than tonal Material containers.
 */
internal val BrandOrange = Color(0xFFFD9527)

// Light theme tokens (models.dev :root)
internal val LightBackground = Color(0xFFFFFFFF)
internal val LightSurface = Color(0xFFF5F5F5)
internal val LightBorder = Color(0xFFDDDDDD)
internal val LightText = Color(0xFF333333)
internal val LightTextSecondary = Color(0xFF666666)
internal val LightTextTertiary = Color(0xFF6E6E6E)
internal val LightBrandText = Color(0xFFB45309)

// Dark theme tokens (models.dev prefers-color-scheme: dark)
internal val DarkBackground = Color(0xFF1E1E1E)
internal val DarkSurface = Color(0xFF111111)
internal val DarkBorder = Color(0xFF333333)
internal val DarkText = Color(0xFFFFFFFF)
internal val DarkTextSecondary = Color(0xFFAAAAAA)
internal val DarkTextTertiary = Color(0xFF9A9A9A)
internal val DarkBrandText = BrandOrange

@Immutable
data class AppPalette(
    val brand: Color,
    val brandText: Color,
    val background: Color,
    val surface: Color,
    val border: Color,
    val textSecondary: Color,
    val textTertiary: Color,
)

internal val LightAppPalette =
    AppPalette(
        brand = BrandOrange,
        brandText = LightBrandText,
        background = LightBackground,
        surface = LightSurface,
        border = LightBorder,
        textSecondary = LightTextSecondary,
        textTertiary = LightTextTertiary,
    )

internal val DarkAppPalette =
    AppPalette(
        brand = BrandOrange,
        brandText = DarkBrandText,
        background = DarkBackground,
        surface = DarkSurface,
        border = DarkBorder,
        textSecondary = DarkTextSecondary,
        textTertiary = DarkTextTertiary,
    )
