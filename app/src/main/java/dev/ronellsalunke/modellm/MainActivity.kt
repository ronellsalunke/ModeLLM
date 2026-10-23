package dev.ronellsalunke.modellm

import android.os.Bundle
import android.view.KeyEvent
import android.view.KeyboardShortcutGroup
import android.view.KeyboardShortcutInfo
import android.view.Menu
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dev.ronellsalunke.modellm.data.settings.ThemeMode
import dev.ronellsalunke.modellm.ui.ModeLLMApp
import dev.ronellsalunke.modellm.ui.settings.SettingsViewModel
import dev.ronellsalunke.modellm.ui.theme.ModeLLMTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.time.Duration.Companion.milliseconds

class MainActivity : ComponentActivity() {
    private val settingsViewModel: SettingsViewModel by viewModel()
    private var splashTimedOut = false

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().setKeepOnScreenCondition {
            !splashTimedOut && !settingsViewModel.isSettingsLoaded.value
        }
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            val loaded =
                withTimeoutOrNull(SPLASH_TIMEOUT_MS.milliseconds) {
                    settingsViewModel.isSettingsLoaded.first { it }
                }
            if (loaded == null) {
                splashTimedOut = true
                window.decorView.invalidate()
            }
        }
        enableEdgeToEdge()
        setContent {
            val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
            val settingsLoaded by settingsViewModel.isSettingsLoaded.collectAsStateWithLifecycle()
            val darkTheme =
                when (settings.themeMode) {
                    ThemeMode.System -> isSystemInDarkTheme()
                    ThemeMode.Light -> false
                    ThemeMode.Dark -> true
                }
            ModeLLMTheme(
                darkTheme = darkTheme,
                dynamicColor = settings.dynamicColor,
                animateThemeChanges = settingsLoaded,
            ) {
                ModeLLMApp()
            }
        }
    }

    override fun onProvideKeyboardShortcuts(
        data: MutableList<KeyboardShortcutGroup>,
        menu: Menu?,
        deviceId: Int,
    ) {
        super.onProvideKeyboardShortcuts(data, menu, deviceId)
        data.add(KeyboardShortcutGroup("Navigation", listOf(
            KeyboardShortcutInfo("Models", KeyEvent.KEYCODE_1, KeyEvent.META_CTRL_ON),
            KeyboardShortcutInfo("Providers", KeyEvent.KEYCODE_2, KeyEvent.META_CTRL_ON),
            KeyboardShortcutInfo("Labs", KeyEvent.KEYCODE_3, KeyEvent.META_CTRL_ON),
            KeyboardShortcutInfo("Find", KeyEvent.KEYCODE_F, KeyEvent.META_CTRL_ON),
            KeyboardShortcutInfo("Refresh", KeyEvent.KEYCODE_R, KeyEvent.META_CTRL_ON),
        )))
    }

    private companion object {
        const val SPLASH_TIMEOUT_MS = 500L
    }
}
