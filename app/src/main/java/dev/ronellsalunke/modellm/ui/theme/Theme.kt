package dev.ronellsalunke.modellm.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val LocalAppPalette = staticCompositionLocalOf { LightAppPalette }
val LocalMaterialYouEnabled = staticCompositionLocalOf { false }

private val LightColorScheme = lightColorScheme(
    primary = BrandOrange,
    onPrimary = Color(0xFF2A1700),
    primaryContainer = Color(0xFFFFE7CC),
    onPrimaryContainer = Color(0xFF4A2A00),
    inversePrimary = Color(0xFFFFB86B),
    secondary = Color(0xFF5F5F5F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = LightSurface,
    onSecondaryContainer = LightText,
    tertiary = Color(0xFF767676),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFECECEC),
    onTertiaryContainer = LightText,
    background = LightBackground,
    onBackground = LightText,
    surface = LightBackground,
    onSurface = LightText,
    surfaceVariant = LightSurface,
    onSurfaceVariant = LightTextSecondary,
    surfaceTint = LightBackground,
    inverseSurface = LightText,
    inverseOnSurface = LightBackground,
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    outline = LightTextTertiary,
    outlineVariant = LightBorder,
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFF5F5F5),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAFAFA),
    surfaceContainer = LightSurface,
    surfaceContainerHigh = Color(0xFFF0F0F0),
    surfaceContainerHighest = Color(0xFFEBEBEB),
)

private val DarkColorScheme = darkColorScheme(
    primary = BrandOrange,
    onPrimary = Color(0xFF2A1700),
    primaryContainer = Color(0xFF4A2A00),
    onPrimaryContainer = Color(0xFFFFDDB3),
    inversePrimary = Color(0xFFC46A00),
    secondary = DarkTextSecondary,
    onSecondary = DarkBackground,
    secondaryContainer = Color(0xFF2A2A2A),
    onSecondaryContainer = DarkText,
    tertiary = Color(0xFF888888),
    onTertiary = DarkBackground,
    tertiaryContainer = Color(0xFF2A2A2A),
    onTertiaryContainer = DarkTextSecondary,
    background = DarkBackground,
    onBackground = DarkText,
    surface = DarkBackground,
    onSurface = DarkText,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = DarkTextSecondary,
    surfaceTint = DarkBackground,
    inverseSurface = DarkText,
    inverseOnSurface = LightText,
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    outline = DarkTextTertiary,
    outlineVariant = DarkBorder,
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF2A2A2A),
    surfaceDim = DarkSurface,
    surfaceContainerLowest = DarkSurface,
    surfaceContainerLow = Color(0xFF161616),
    surfaceContainer = Color(0xFF1A1A1A),
    surfaceContainerHigh = Color(0xFF232323),
    surfaceContainerHighest = Color(0xFF2A2A2A),
)

@Composable
fun ModeLLMTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val useDynamicColor = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val colorScheme = when {
        useDynamicColor ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val palette = if (useDynamicColor) {
        AppPalette(
            brand = colorScheme.primary,
            brandText = colorScheme.primary,
            background = colorScheme.background,
            surface = colorScheme.surfaceContainer,
            border = colorScheme.outlineVariant,
            textSecondary = colorScheme.onSurfaceVariant,
            textTertiary = colorScheme.outline,
        )
    } else {
        if (darkTheme) DarkAppPalette else LightAppPalette
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
            @Suppress("DEPRECATION")
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                    window.isNavigationBarContrastEnforced = false
                    window.navigationBarColor = android.graphics.Color.TRANSPARENT
                }

                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    window.navigationBarColor = colorScheme.background.toArgb()
                }

                else -> {
                    window.navigationBarColor = DarkBackground.toArgb()
                }
            }
        }
    }

    CompositionLocalProvider(
        LocalAppPalette provides palette,
        LocalMaterialYouEnabled provides useDynamicColor,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = ModelsDevShapes,
            content = content,
        )
    }
}
