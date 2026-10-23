package dev.ronellsalunke.modellm.ui.models

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ronellsalunke.modellm.R
import dev.ronellsalunke.modellm.data.model.CanonicalModel
import dev.ronellsalunke.modellm.data.model.ModelFilters
import dev.ronellsalunke.modellm.data.model.ModelSort
import dev.ronellsalunke.modellm.data.repository.SyncState
import dev.ronellsalunke.modellm.ui.components.AppPullToRefreshBox
import dev.ronellsalunke.modellm.ui.components.CanonicalModelRow
import dev.ronellsalunke.modellm.ui.components.CatalogDivider
import dev.ronellsalunke.modellm.ui.components.EmptyState
import dev.ronellsalunke.modellm.ui.components.ErrorState
import dev.ronellsalunke.modellm.ui.components.MinGridCellWidth
import dev.ronellsalunke.modellm.ui.components.navigationAreaBottomPadding
import dev.ronellsalunke.modellm.ui.components.HomeTopAppBar
import dev.ronellsalunke.modellm.ui.components.SearchField
import dev.ronellsalunke.modellm.ui.components.SyncBanner
import dev.ronellsalunke.modellm.ui.components.TooltipIconButton
import dev.ronellsalunke.modellm.ui.components.catalogShortcuts
import dev.ronellsalunke.modellm.ui.components.dismissCatalogFocus
import dev.ronellsalunke.modellm.ui.components.navigationBarBottomPadding
import dev.ronellsalunke.modellm.ui.components.tabScreenPadding
import dev.ronellsalunke.modellm.ui.theme.Dimens
import dev.ronellsalunke.modellm.ui.theme.LocalAppPalette
import org.koin.androidx.compose.koinViewModel

private val ChipCloseIconSize = 16.dp

private fun ModelSort.label(): String =
    when (this) {
        ModelSort.Newest -> "Newest"
        ModelSort.Cheapest -> "Cheapest"
        ModelSort.LargestContext -> "Largest context"
        ModelSort.Name -> "Name"
    }

@Composable
fun ModelsListScreen(
    onModelClick: (CanonicalModel) -> Unit,
    onSettings: () -> Unit,
    viewModel: ModelsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(Unit) { viewModel.setQuery(query) }
    var filterSheet by rememberSaveable { mutableStateOf(false) }
    val dismissKeyboard = dismissCatalogFocus()
    val activeFilters = remember(state.filters, state.providers, state.labs) {
        state.filters.activeFilters(state.providers, state.labs)
    }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val searchFocusRequester = remember { FocusRequester() }
    Column(
        Modifier.tabScreenPadding().background(LocalAppPalette.current.background)
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .catalogShortcuts(searchFocusRequester, viewModel::refresh),
    ) {
        ModelsTopBar(
            sort = state.sort,
            scrollBehavior = scrollBehavior,
            activeFiltersCount = activeFilters.size,
            onSortSelected = viewModel::setSort,
            onOpenFilters = { filterSheet = true },
            onSettings = onSettings,
            onDismissKeyboard = dismissKeyboard,
        )
        CatalogDivider()
        ModelsSearchField(
            focusRequester = searchFocusRequester,
            query = query,
            onQueryChange = {
                query = it
                viewModel.setQuery(it)
            },
            filtersVisible = activeFilters.isNotEmpty(),
        )
        ActiveFiltersRow(activeFilters = activeFilters, onRemoveFilter = viewModel::setFilters)
        SyncBanner(state.lastSyncedAt, state.syncState, viewModel::refresh)
        ModelsListContent(
            state = state,
            hasSearchOrFilters = query.isNotBlank() || activeFilters.isNotEmpty(),
            onClear = {
                query = ""
                viewModel.setQuery("")
                viewModel.clearFilters()
            },
            onRefresh = viewModel::refresh,
            onModelClick = onModelClick,
            modifier = Modifier.weight(1f),
        )
    }
    if (filterSheet) {
        ModelFiltersSheet(
            filters = state.filters,
            providers = state.providers,
            labs = state.labs,
            onDismiss = { filterSheet = false },
            onUpdateFilters = viewModel::updateFilters,
        )
    }
}

@Composable
private fun ModelsSearchField(
    focusRequester: FocusRequester,
    query: String,
    onQueryChange: (String) -> Unit,
    filtersVisible: Boolean,
) {
    SearchField(
        focusRequester = focusRequester,
        query = query,
        onQueryChange = onQueryChange,
        label = "Search models, labs, providers",
        modifier =
            Modifier.fillMaxWidth()
                .padding(
                    start = Dimens.sm,
                    top = Dimens.sm,
                    end = Dimens.sm,
                    // The chip row adds an 8dp touch-target inset above its visible chip.
                    bottom = if (filtersVisible) 0.dp else Dimens.sm,
                ),
    )
}

@Composable
private fun ModelsTopBar(
    sort: ModelSort,
    scrollBehavior: TopAppBarScrollBehavior,
    activeFiltersCount: Int,
    onSortSelected: (ModelSort) -> Unit,
    onOpenFilters: () -> Unit,
    onSettings: () -> Unit,
    onDismissKeyboard: () -> Unit,
) {
    HomeTopAppBar(
        title = "ModeLLM",
        scrollBehavior = scrollBehavior,
        leading = {
            Box(Modifier.width(Dimens.xxl + Dimens.sm), contentAlignment = Alignment.Center) {
                Icon(
                    painterResource(R.drawable.ic_modellm_logo),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(Dimens.xl),
                )
            }
        },
        actions = {
            SortDropdownAction(sort, onSortSelected, onDismissKeyboard)
            FilterBadgeAction(activeFiltersCount) {
                onDismissKeyboard()
                onOpenFilters()
            }
            TooltipIconButton(text = "Settings", onClick = onSettings) {
                Icon(Icons.Default.Settings, "Settings")
            }
        },
    )
}

@Composable
private fun SortDropdownAction(
    currentSort: ModelSort,
    onSortSelected: (ModelSort) -> Unit,
    onBeforeOpen: () -> Unit,
) {
    var sortMenu by remember { mutableStateOf(false) }
    Box {
        TooltipIconButton(
            text = "Sort by ${currentSort.label()}",
            onClick = {
                onBeforeOpen()
                sortMenu = true
            }
        ) {
            Icon(
                Icons.Default.SwapVert,
                contentDescription = "Sort by ${currentSort.label()}",
            )
        }
        DropdownMenu(
            expanded = sortMenu,
            onDismissRequest = { sortMenu = false },
            shape = MaterialTheme.shapes.medium,
        ) {
            ModelSort.entries.forEach { sort ->
                DropdownMenuItem(
                    text = { Text(sort.label()) },
                    onClick = {
                        onSortSelected(sort)
                        sortMenu = false
                    },
                    trailingIcon = {
                        if (sort == currentSort) {
                            Icon(Icons.Default.Check, contentDescription = "Selected")
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun FilterBadgeAction(
    activeFilterCount: Int,
    onClick: () -> Unit,
) {
    TooltipIconButton(
        text = if (activeFilterCount == 0) "Filters" else "Filters, $activeFilterCount active",
        onClick = onClick,
    ) {
        BadgedBox(
            badge = {
                if (activeFilterCount > 0) {
                    Badge { Text(activeFilterCount.toString()) }
                }
            }
        ) {
            Icon(
                Icons.Default.FilterList,
                contentDescription =
                    if (activeFilterCount == 0) "Filters"
                    else "Filters, $activeFilterCount active",
            )
        }
    }
}

@Composable
private fun ActiveFiltersRow(
    activeFilters: List<ActiveFilter>,
    onRemoveFilter: (ModelFilters) -> Unit,
) {
    if (activeFilters.isEmpty()) return
    LazyRow(
        contentPadding = PaddingValues(horizontal = Dimens.sm),
        horizontalArrangement = Arrangement.spacedBy(Dimens.sm),
    ) {
        items(activeFilters, key = { it.key }) { filter ->
            InputChip(
                selected = true,
                onClick = { onRemoveFilter(filter.remove) },
                shape = MaterialTheme.shapes.small,
                label = { Text(filter.label) },
                trailingIcon = {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove ${filter.label}",
                        modifier = Modifier.size(ChipCloseIconSize),
                    )
                },
            )
        }
    }
}

@Composable
private fun ModelsListContent(
    state: ModelsUiState,
    hasSearchOrFilters: Boolean,
    onClear: () -> Unit,
    onRefresh: () -> Unit,
    onModelClick: (CanonicalModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    AppPullToRefreshBox(
        isRefreshing = state.syncState is SyncState.Running,
        onRefresh = onRefresh,
        modifier = modifier,
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(MinGridCellWidth),
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    bottom = navigationAreaBottomPadding() + navigationBarBottomPadding(),
                ),
        ) {
            if (state.models.isNullOrEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    ModelsListEmptyState(
                        state = state,
                        hasSearchOrFilters = hasSearchOrFilters,
                        onClear = onClear,
                        onRefresh = onRefresh,
                    )
                }
            }
            items(
                state.models.orEmpty(),
                key = { it.id },
                contentType = { "model" },
            ) { model ->
                CanonicalModelRow(model) { onModelClick(model) }
            }
        }
    }
}

@Composable
private fun ModelsListEmptyState(
    state: ModelsUiState,
    hasSearchOrFilters: Boolean,
    onClear: () -> Unit,
    onRefresh: () -> Unit,
) {
    val failure = state.syncState as? SyncState.Failed
    when {
        state.models == null -> EmptyState("Loading models…")
        state.lastSyncedAt == null && state.providers.isEmpty() -> {
            if (failure != null) {
                ErrorState(failure.message, onRefresh)
            } else {
                EmptyState(
                    "Downloading the model catalogue…",
                    "Connect to the internet to download the catalog " +
                        "for offline browsing.",
                )
            }
        }
        hasSearchOrFilters ->
            EmptyState(
                "No models match",
                "Try another search or change your filters.",
                "Clear search and filters",
                onClear,
            )
        else ->
            EmptyState(
                "No models in the catalog",
                actionLabel = "Retry",
                onAction = onRefresh,
            )
    }
}
