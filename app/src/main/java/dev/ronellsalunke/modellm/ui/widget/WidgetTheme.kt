package dev.ronellsalunke.modellm.ui.widget

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.glance.GlanceTheme
import androidx.glance.color.ColorProviders as GlanceColorProviders
import androidx.glance.material3.ColorProviders
import dev.ronellsalunke.modellm.data.settings.AppSettings
import dev.ronellsalunke.modellm.ui.theme.DarkColorScheme
import dev.ronellsalunke.modellm.ui.theme.LightColorScheme

/**
 * Dynamic colors use Glance's resource-backed theme so the launcher can re-resolve wallpaper
 * colors. The default schemes are fixed day/night pairs for the app's own palette.
 */
@Composable
internal fun widgetColors(settings: AppSettings): GlanceColorProviders =
    if (settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        GlanceTheme.colors
    } else {
        ColorProviders(LightColorScheme, DarkColorScheme)
    }
