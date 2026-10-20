package dev.ronellsalunke.modellm.ui.labs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.ronellsalunke.modellm.data.model.CanonicalModel
import dev.ronellsalunke.modellm.data.model.Lab
import dev.ronellsalunke.modellm.data.repository.CatalogRepository
import dev.ronellsalunke.modellm.data.repository.SyncState
import dev.ronellsalunke.modellm.data.sync.CatalogSyncScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class LabsUiState(
    val labs: List<Lab>? = null,
    val syncState: SyncState = SyncState.Idle,
    val lastSyncedAt: Long? = null,
)

class LabsViewModel(repository: CatalogRepository, private val scheduler: CatalogSyncScheduler) :
    ViewModel() {
    val uiState =
        combine(
                repository.observeLabs(),
                repository.syncState,
                repository.observeLastSyncedAt(),
                ::LabsUiState,
            )
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LabsUiState())

    fun refresh() = scheduler.syncNow()
}

data class LabDetailUiState(
    val lab: Lab? = null,
    val models: List<CanonicalModel> = emptyList(),
    val loading: Boolean = true,
    val syncState: SyncState = SyncState.Idle,
)

class LabDetailViewModel(
    labId: String,
    repository: CatalogRepository,
    private val scheduler: CatalogSyncScheduler,
) : ViewModel() {
    val uiState =
        combine(
                repository.observeLab(labId),
                repository.observeLabModels(labId),
                repository.syncState,
            ) { lab, models, sync ->
                LabDetailUiState(lab, models, false, sync)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LabDetailUiState())

    fun refresh() = scheduler.syncNow()
}
