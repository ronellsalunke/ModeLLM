package dev.ronellsalunke.modellm.ui.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.ronellsalunke.modellm.data.model.CanonicalModel
import dev.ronellsalunke.modellm.data.model.Model
import dev.ronellsalunke.modellm.data.repository.CatalogRepository
import dev.ronellsalunke.modellm.data.repository.SyncState
import dev.ronellsalunke.modellm.data.sync.CatalogSyncScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class CanonicalModelDetailUiState(
    val model: CanonicalModel? = null,
    val providers: List<Model> = emptyList(),
    val loading: Boolean = true,
    val syncState: SyncState = SyncState.Idle,
)

class CanonicalModelDetailViewModel(
    canonicalId: String,
    repository: CatalogRepository,
    private val scheduler: CatalogSyncScheduler,
) : ViewModel() {
    val uiState =
        combine(
                repository.observeCanonicalModel(canonicalId),
                repository.observeCanonicalProviders(canonicalId),
                repository.syncState,
            ) { model, providers, sync ->
                CanonicalModelDetailUiState(model, providers, false, sync)
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                CanonicalModelDetailUiState(),
            )

    fun refresh() = scheduler.syncNow()
}
