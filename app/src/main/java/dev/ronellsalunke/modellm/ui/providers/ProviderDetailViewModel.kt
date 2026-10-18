package dev.ronellsalunke.modellm.ui.providers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.ronellsalunke.modellm.data.model.Model
import dev.ronellsalunke.modellm.data.model.Provider
import dev.ronellsalunke.modellm.data.repository.CatalogRepository
import dev.ronellsalunke.modellm.data.repository.SyncState
import dev.ronellsalunke.modellm.data.sync.CatalogSyncScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ProviderDetailUiState(
    val provider: Provider? = null,
    val models: List<Model> = emptyList(),
    val loading: Boolean = true,
    val syncState: SyncState = SyncState.Idle,
)

class ProviderDetailViewModel(
    providerId: String,
    repository: CatalogRepository,
    private val scheduler: CatalogSyncScheduler,
) : ViewModel() {
    val uiState = combine(
        repository.observeProvider(providerId), repository.observeModelsByProvider(providerId), repository.syncState,
    ) { provider, models, sync -> ProviderDetailUiState(provider, models, false, sync) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProviderDetailUiState())

    fun refresh() = scheduler.syncNow()
}
