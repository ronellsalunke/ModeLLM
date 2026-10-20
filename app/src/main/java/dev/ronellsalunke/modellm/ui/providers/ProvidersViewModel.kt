package dev.ronellsalunke.modellm.ui.providers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.ronellsalunke.modellm.data.model.Provider
import dev.ronellsalunke.modellm.data.repository.CatalogRepository
import dev.ronellsalunke.modellm.data.repository.SyncState
import dev.ronellsalunke.modellm.data.sync.CatalogSyncScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

data class ProvidersUiState(
    val providers: List<Provider> = emptyList(),
    val loading: Boolean = true,
    val syncState: SyncState = SyncState.Idle,
    val lastSyncedAt: Long? = null,
)

class ProvidersViewModel(
    repository: CatalogRepository,
    private val scheduler: CatalogSyncScheduler,
) : ViewModel() {
    val uiState =
        combine(
                repository.observeProviders(),
                repository.syncState,
                repository.observeLastSyncedAt(),
            ) { providers, sync, last ->
                ProvidersUiState(providers, false, sync, last)
            }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProvidersUiState())

    fun refresh() = scheduler.syncNow()
}
