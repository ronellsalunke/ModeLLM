package dev.ronellsalunke.modellm.ui.labs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ronellsalunke.modellm.data.repository.SyncState
import dev.ronellsalunke.modellm.ui.components.*
import dev.ronellsalunke.modellm.ui.theme.*
import dev.ronellsalunke.modellm.ui.util.pluralized
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabsScreen(onLabClick: (String) -> Unit, viewModel: LabsViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    val filteredLabs = remember(state.labs, query) {
        state.labs.orEmpty().filter { query.isBlank() || it.name.contains(query, true) || it.id.contains(query, true) }
    }
    Column(Modifier.fillMaxSize().background(LocalAppPalette.current.background)) {
        AppHeader("LABS")
        SearchField(query, { query = it }, "Search labs", Modifier.fillMaxWidth().padding(Dimens.sm))
        SyncBanner(state.lastSyncedAt, state.syncState, viewModel::refresh)
        AppPullToRefreshBox(state.syncState is SyncState.Running, viewModel::refresh, Modifier.weight(1f)) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = FloatingTabBarClearance)) {
                if (filteredLabs.isEmpty()) item {
                    val failure = state.syncState as? SyncState.Failed
                    when {
                        failure != null && state.labs.isNullOrEmpty() -> ErrorState(failure.message, viewModel::refresh)
                        state.labs == null || (state.labs.isNullOrEmpty() && state.lastSyncedAt == null) -> EmptyState("Loading labs…")
                        query.isNotBlank() -> EmptyState("No labs match", actionLabel = "Clear search", onAction = { query = "" })
                        else -> EmptyState("No labs in the catalog", actionLabel = "Retry", onAction = viewModel::refresh)
                    }
                }
                items(filteredLabs, key = { it.id }) { lab ->
                    Column(Modifier.fillMaxWidth().clickable { onLabClick(lab.id) }.padding(Dimens.lg), verticalArrangement = Arrangement.spacedBy(Dimens.xs)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                            LabLogo(lab.id, lab.name)
                            Text(lab.name, style = MaterialTheme.typography.titleSmall)
                        }
                        Text("${pluralized(lab.modelCount, "model")} · ${pluralized(lab.providerCount, "provider")} · ${lab.lastUpdated ?: "–"}", style = MonoLabelStyle, color = LocalAppPalette.current.textTertiary)
                    }
                    CatalogDivider()
                }
            }
        }
    }
}

@Composable
fun LabDetailScreen(labId: String, onBack: () -> Unit, onModelClick: (String) -> Unit) {
    val viewModel = koinViewModel<LabDetailViewModel>(key = labId, parameters = { parametersOf(labId) })
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lab = state.lab
    Column(Modifier.fillMaxSize().background(LocalAppPalette.current.background)) {
        BackHeader("Labs / ${lab?.name ?: labId}", onBack)
        if (lab == null) {
            when {
                state.loading -> EmptyState("Loading lab…")
                state.syncState is SyncState.Failed -> ErrorState((state.syncState as SyncState.Failed).message, viewModel::refresh)
                else -> EmptyState("Lab not found")
            }
        } else LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = Dimens.xl)) {
            item {
                Column(Modifier.padding(Dimens.lg), verticalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                        LabLogo(lab.id, lab.name)
                        Text(lab.name, style = MaterialTheme.typography.headlineLarge)
                    }
                    Text(lab.id, style = MonoStyle, color = LocalAppPalette.current.textTertiary)
                    lab.description?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
                }
                FactGrid(listOf(Fact("Models", lab.modelCount.toString(), true), Fact("Providers", lab.providerCount.toString(), true), Fact("Updated", lab.lastUpdated)))
                SectionHeader("Models (${lab.modelCount})")
                if (state.models.isEmpty()) EmptyState("No models listed")
            }
            items(state.models, key = { it.id }) { model -> CanonicalModelRow(model) { onModelClick(model.id) } }
        }
    }
}
