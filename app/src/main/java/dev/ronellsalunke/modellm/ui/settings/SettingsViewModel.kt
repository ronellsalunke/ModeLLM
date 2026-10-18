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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val syncState: SyncState = SyncState.Idle,
    val lastSyncedAt: Long? = null,
    val error: String? = null,
)

class SettingsViewModel(
    private val repository: SettingsRepository,
    catalog: CatalogRepository,
    private val scheduler: CatalogSyncScheduler,
) : ViewModel() {
    private val error = MutableStateFlow<String?>(null)
    val uiState = combine(repository.settings, catalog.syncState, catalog.observeLastSyncedAt(), error) {
            settings, sync, last, error -> SettingsUiState(settings, sync, last, error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

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
