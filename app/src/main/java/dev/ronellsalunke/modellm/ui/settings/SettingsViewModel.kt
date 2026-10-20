package dev.ronellsalunke.modellm.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.ronellsalunke.modellm.data.repository.CatalogRepository
import dev.ronellsalunke.modellm.data.repository.SyncState
import dev.ronellsalunke.modellm.data.settings.AppSettings
import dev.ronellsalunke.modellm.data.settings.SettingsRepository
import dev.ronellsalunke.modellm.data.settings.ThemeMode
import dev.ronellsalunke.modellm.data.sync.CatalogSyncScheduler
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val syncState: SyncState = SyncState.Idle,
    val lastSyncedAt: Long? = null,
    val error: String? = null,
    val settingsLoaded: Boolean = false,
)

class SettingsViewModel(
    private val repository: SettingsRepository,
    catalog: CatalogRepository,
    private val scheduler: CatalogSyncScheduler,
) : ViewModel() {
    private val error = MutableStateFlow<String?>(null)
    private val settingsLoaded = MutableStateFlow(false)

    val settings: StateFlow<AppSettings> =
        repository.settings
            .onEach { settingsLoaded.value = true }
            .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val isSettingsLoaded: StateFlow<Boolean> = settingsLoaded.asStateFlow()

    val uiState =
        combine(
                settings,
                catalog.syncState,
                catalog.observeLastSyncedAt(),
                error,
                settingsLoaded,
            ) { settings, sync, last, error, loaded ->
                SettingsUiState(settings, sync, last, error, loaded)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setThemeMode(mode: ThemeMode) = save { repository.setThemeMode(mode) }

    fun setDynamicColor(enabled: Boolean) = save { repository.setDynamicColor(enabled) }

    private fun save(action: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                action()
                error.value = null
            } catch (_: IOException) {
                error.value = "Couldn't save your settings. Try again."
            }
        }
    }

    fun refresh() = scheduler.syncNow()
}
