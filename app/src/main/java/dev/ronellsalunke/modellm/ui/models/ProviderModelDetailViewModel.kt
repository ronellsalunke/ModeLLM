package dev.ronellsalunke.modellm.ui.models

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

data class ProviderModelDetailUiState(
    val model: Model? = null,
    val provider: Provider? = null,
    val loading: Boolean = true,
    val syncState: SyncState = SyncState.Idle,
)

class ProviderModelDetailViewModel(
    providerId: String,
    modelId: String,
    repository: CatalogRepository,
    private val scheduler: CatalogSyncScheduler,
) : ViewModel() {
    val uiState =
        combine(
                repository.observeModel(providerId, modelId),
                repository.observeProvider(providerId),
                repository.syncState,
            ) { model, provider, sync ->
                ProviderModelDetailUiState(model, provider, false, sync)
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                ProviderModelDetailUiState(),
            )

    fun refresh() = scheduler.syncNow()
}
