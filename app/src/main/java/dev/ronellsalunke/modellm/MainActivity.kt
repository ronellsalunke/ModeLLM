package dev.ronellsalunke.modellm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ronellsalunke.modellm.data.settings.ThemeMode
import dev.ronellsalunke.modellm.ui.ModeLLMApp
import dev.ronellsalunke.modellm.ui.settings.SettingsViewModel
import dev.ronellsalunke.modellm.ui.theme.ModeLLMTheme
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel = koinViewModel<SettingsViewModel>()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val darkTheme = when (state.settings.themeMode) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }
            ModeLLMTheme(darkTheme = darkTheme, dynamicColor = state.settings.dynamicColor) { ModeLLMApp() }
        }
    }
}
