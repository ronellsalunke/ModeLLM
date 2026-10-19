package dev.ronellsalunke.modellm.ui.models

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ronellsalunke.modellm.data.model.*
import dev.ronellsalunke.modellm.data.repository.SyncState
import dev.ronellsalunke.modellm.ui.components.*
import dev.ronellsalunke.modellm.ui.theme.Dimens
import dev.ronellsalunke.modellm.ui.theme.LocalAppPalette
import org.koin.androidx.compose.koinViewModel

private fun ModelSort.label(): String = when (this) {
    ModelSort.Newest -> "Newest"
    ModelSort.Cheapest -> "Cheapest"
    ModelSort.LargestContext -> "Largest context"
    ModelSort.Name -> "Name"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelsListScreen(onModelClick: (CanonicalModel) -> Unit, onSettings: () -> Unit, viewModel: ModelsViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(Unit) { viewModel.setQuery(query) }
    var sortMenu by remember { mutableStateOf(false) }
    var filterSheet by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val activeFilters = remember(state.filters, state.providers, state.labs) { state.filters.activeFilters(state.providers, state.labs) }
    Column(Modifier.fillMaxSize()) {
        AppHeader("MODELLM") {
            IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "Settings") }
        }
        Row(Modifier.fillMaxWidth().padding(Dimens.sm), horizontalArrangement = Arrangement.spacedBy(Dimens.xs)) {
            SearchField(query, { query = it; viewModel.setQuery(it) }, "Search models, labs, providers", Modifier.weight(1f))
            Box {
                OutlinedButton(onClick = { sortMenu = true }, modifier = Modifier.height(48.dp),
                    shape = MaterialTheme.shapes.small, border = BorderStroke(1.dp, LocalAppPalette.current.border),
                    contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Icon(Icons.Default.SwapVert, contentDescription = "Sort", modifier = Modifier.size(20.dp))
                    Text(when (state.sort) {
                        ModelSort.LargestContext -> "Context"
                        ModelSort.Name -> "Name"
                        else -> state.sort.label()
                    }, style = MaterialTheme.typography.labelSmall)
                }
                DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }, shape = MaterialTheme.shapes.medium) {
                    ModelSort.entries.forEach { sort ->
                        DropdownMenuItem(text = { Text(sort.label()) }, onClick = { viewModel.setSort(sort); sortMenu = false },
                            trailingIcon = { if (sort == state.sort) Icon(Icons.Default.Check, contentDescription = "Selected") })
                    }
                }
            }
            OutlinedButton(onClick = { focusManager.clearFocus(); keyboardController?.hide(); filterSheet = true }, contentPadding = PaddingValues(Dimens.sm),
                border = BorderStroke(1.dp, LocalAppPalette.current.border),
                shape = MaterialTheme.shapes.small, modifier = Modifier.height(48.dp)) {
                Icon(Icons.Default.FilterList, contentDescription = "Filters", modifier = Modifier.size(20.dp))
                if (activeFilters.isNotEmpty()) Text(" ${activeFilters.size}")
            }
        }
        if (activeFilters.isNotEmpty()) LazyRow(contentPadding = PaddingValues(horizontal = Dimens.sm), horizontalArrangement = Arrangement.spacedBy(Dimens.sm)) {
            items(activeFilters, key = { it.key }) { filter ->
                InputChip(selected = true, onClick = { viewModel.setFilters(filter.remove) }, shape = MaterialTheme.shapes.small,
                    label = { Text(filter.label) }, trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Remove ${filter.label}", modifier = Modifier.size(16.dp)) })
            }
        }
        SyncBanner(state.lastSyncedAt, state.syncState, viewModel::refresh)
        AppPullToRefreshBox(isRefreshing = state.syncState is SyncState.Running, onRefresh = viewModel::refresh, modifier = Modifier.weight(1f)) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = FloatingTabBarClearance)) {
                if (state.models.isNullOrEmpty()) item {
                    when {
                        state.models == null -> EmptyState("Loading models…")
                        state.lastSyncedAt == null && state.providers.isEmpty() -> {
                            val failure = state.syncState as? SyncState.Failed
                            if (failure != null) ErrorState(failure.message, viewModel::refresh)
                            else EmptyState("Downloading the model catalogue…", "Connect to the internet to download the catalog for offline browsing.")
                        }
                        query.isNotBlank() || activeFilters.isNotEmpty() -> EmptyState("No models match", "Try another search or change your filters.", "Clear search and filters", {
                            query = ""; viewModel.setQuery(""); viewModel.clearFilters()
                        })
                        else -> EmptyState("No models in the catalog", actionLabel = "Retry", onAction = viewModel::refresh)
                    }
                }
                items(state.models.orEmpty(), key = { it.id }, contentType = { "model" }) { model ->
                    CanonicalModelRow(model) { onModelClick(model) }
                }
            }
        }
    }
    if (filterSheet) ModelFiltersSheet(state.filters, state.providers, state.labs,
        onDismiss = { filterSheet = false }, onUpdateFilters = viewModel::updateFilters)
}
