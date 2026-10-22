package dev.ronellsalunke.modellm.ui.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.ronellsalunke.modellm.data.model.CanonicalModel
import dev.ronellsalunke.modellm.data.model.Lab
import dev.ronellsalunke.modellm.data.model.ModelFilters
import dev.ronellsalunke.modellm.data.model.ModelSort
import dev.ronellsalunke.modellm.data.model.Provider
import dev.ronellsalunke.modellm.data.repository.CatalogRepository
import dev.ronellsalunke.modellm.data.repository.SyncState
import dev.ronellsalunke.modellm.data.sync.CatalogSyncScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlin.time.Duration.Companion.milliseconds

data class ModelsUiState(
    val filters: ModelFilters = ModelFilters(),
    val sort: ModelSort = ModelSort.Newest,
    val models: List<CanonicalModel>? = null,
    val providers: List<Provider> = emptyList(),
    val labs: List<Lab> = emptyList(),
    val syncState: SyncState = SyncState.Idle,
    val lastSyncedAt: Long? = null,
)

private data class ModelSelection(
    val query: String,
    val filters: ModelFilters,
    val sort: ModelSort,
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class ModelsViewModel(
    repository: CatalogRepository,
    private val scheduler: CatalogSyncScheduler,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val filters = MutableStateFlow(ModelFilters())
    private val sort = MutableStateFlow(ModelSort.Newest)
    private val selection =
        combine(query.debounce(150.milliseconds), filters, sort, ::ModelSelection)
    private val results =
        selection
            .flatMapLatest { selected ->
                repository.observeCanonicalModels(selected.query, selected.filters, selected.sort)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    private val catalog =
        combine(
            repository.observeProviders(),
            repository.syncState,
            repository.observeLastSyncedAt(),
        ) { providers, sync, last ->
            Triple(providers, sync, last)
        }

    val uiState =
        combine(filters, sort, results, catalog, repository.observeLabs()) {
                filters,
                sort,
                result,
                catalog,
                labs ->
                ModelsUiState(
                    filters,
                    sort,
                    result,
                    catalog.first,
                    labs,
                    catalog.second,
                    catalog.third,
                )
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ModelsUiState())

    fun setQuery(value: String) {
        query.value = value
    }

    fun setFilters(value: ModelFilters) {
        filters.value = value
    }

    fun updateFilters(transform: (ModelFilters) -> ModelFilters) {
        filters.update(transform)
    }

    fun setSort(value: ModelSort) {
        sort.value = value
    }

    fun clearFilters() {
        filters.value = ModelFilters()
    }

    fun refresh() = scheduler.syncNow()
}
