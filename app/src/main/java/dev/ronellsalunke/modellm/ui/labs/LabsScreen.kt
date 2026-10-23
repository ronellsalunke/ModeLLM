package dev.ronellsalunke.modellm.ui.labs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ronellsalunke.modellm.data.model.Lab
import dev.ronellsalunke.modellm.data.repository.SyncState
import dev.ronellsalunke.modellm.ui.components.AppPullToRefreshBox
import dev.ronellsalunke.modellm.ui.components.CanonicalModelRow
import dev.ronellsalunke.modellm.ui.components.CatalogDivider
import dev.ronellsalunke.modellm.ui.components.CatalogRowMinHeight
import dev.ronellsalunke.modellm.ui.components.DetailTopAppBar
import dev.ronellsalunke.modellm.ui.components.EmptyState
import dev.ronellsalunke.modellm.ui.components.ErrorState
import dev.ronellsalunke.modellm.ui.components.Fact
import dev.ronellsalunke.modellm.ui.components.FactGrid
import dev.ronellsalunke.modellm.ui.components.MinGridCellWidth
import dev.ronellsalunke.modellm.ui.components.navigationAreaBottomPadding
import dev.ronellsalunke.modellm.ui.components.HomeTopAppBar
import dev.ronellsalunke.modellm.ui.components.LabLogo
import dev.ronellsalunke.modellm.ui.components.SearchField
import dev.ronellsalunke.modellm.ui.components.SectionHeader
import dev.ronellsalunke.modellm.ui.components.ShareIconButton
import dev.ronellsalunke.modellm.ui.components.SyncBanner
import dev.ronellsalunke.modellm.ui.components.catalogShortcuts
import dev.ronellsalunke.modellm.ui.components.navigationBarBottomPadding
import dev.ronellsalunke.modellm.ui.components.safeDrawingBottomPadding
import dev.ronellsalunke.modellm.ui.components.safeDrawingHorizontalPadding
import dev.ronellsalunke.modellm.ui.components.tabScreenPadding
import dev.ronellsalunke.modellm.ui.theme.Dimens
import dev.ronellsalunke.modellm.ui.theme.LocalAppPalette
import dev.ronellsalunke.modellm.ui.theme.MonoLabelStyle
import dev.ronellsalunke.modellm.ui.theme.MonoStyle
import dev.ronellsalunke.modellm.ui.util.pluralized
import dev.ronellsalunke.modellm.ui.util.ModelsDevLinks
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun LabsScreen(onLabClick: (String) -> Unit, viewModel: LabsViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    val filteredLabs =
        remember(state.labs, query) {
            state.labs.orEmpty().filter {
                query.isBlank() || it.name.contains(query, true) || it.id.contains(query, true)
            }
        }
    val searchFocusRequester = remember { FocusRequester() }
    Column(
        Modifier.tabScreenPadding().background(LocalAppPalette.current.background)
            .catalogShortcuts(searchFocusRequester, viewModel::refresh)
    ) {
        HomeTopAppBar(title = "Labs")
        CatalogDivider()
        SearchField(
            query,
            { query = it },
            "Search labs",
            Modifier.fillMaxWidth().padding(Dimens.sm),
            focusRequester = searchFocusRequester,
        )
        SyncBanner(state.lastSyncedAt, state.syncState, viewModel::refresh)
        AppPullToRefreshBox(
            state.syncState is SyncState.Running,
            viewModel::refresh,
            Modifier.weight(1f),
        ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(MinGridCellWidth),
                modifier = Modifier.fillMaxSize(),
                contentPadding =
                    PaddingValues(
                        bottom = navigationAreaBottomPadding() + navigationBarBottomPadding(),
                    ),
            ) {
                if (filteredLabs.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        LabsEmptyState(
                            syncState = state.syncState,
                            labs = state.labs,
                            lastSyncedAt = state.lastSyncedAt,
                            query = query,
                            onClearQuery = { query = "" },
                            onRetry = viewModel::refresh,
                        )
                    }
                }
                items(filteredLabs, key = { it.id }) { lab ->
                    LabRow(lab = lab, onLabClick = onLabClick)
                }
            }
        }
    }
}

@Composable
private fun LabsEmptyState(
    syncState: SyncState,
    labs: List<Lab>?,
    lastSyncedAt: Long?,
    query: String,
    onClearQuery: () -> Unit,
    onRetry: () -> Unit,
) {
    val failure = syncState as? SyncState.Failed
    when {
        failure != null && labs.isNullOrEmpty() ->
            ErrorState(failure.message, onRetry)
        labs == null || (labs.isEmpty() && lastSyncedAt == null) ->
            EmptyState("Loading labs…")
        query.isNotBlank() ->
            EmptyState(
                "No labs match",
                actionLabel = "Clear search",
                onAction = onClearQuery,
            )
        else ->
            EmptyState(
                "No labs in the catalog",
                actionLabel = "Retry",
                onAction = onRetry,
            )
    }
}

@Composable
private fun LabRow(lab: Lab, onLabClick: (String) -> Unit) {
    val modelCountText = pluralized(lab.modelCount, "model")
    val providerCountText = pluralized(lab.providerCount, "provider")
    val updatedText = lab.lastUpdated ?: "–"
    Column(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth()
                .heightIn(min = CatalogRowMinHeight)
                .clickable { onLabClick(lab.id) }
                .padding(horizontal = Dimens.lg, vertical = Dimens.sm),
            verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.sm),
            ) {
                LabLogo(lab.id, lab.name)
                Text(
                    lab.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                "$modelCountText · $providerCountText · $updatedText",
                style = MonoLabelStyle,
                color = LocalAppPalette.current.textTertiary,
            )
        }
        CatalogDivider()
    }
}

@Composable
fun LabDetailScreen(labId: String, onBack: () -> Unit, onModelClick: (String) -> Unit) {
    val viewModel =
        koinViewModel<LabDetailViewModel>(key = labId, parameters = { parametersOf(labId) })
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lab = state.lab
    val listPadding = PaddingValues(
        bottom = Dimens.xl + safeDrawingBottomPadding() + navigationAreaBottomPadding(),
    )
    Column(
        Modifier.fillMaxSize()
            .background(LocalAppPalette.current.background)
            .safeDrawingHorizontalPadding()
    ) {
        DetailTopAppBar(
            title = lab?.name ?: labId,
            onBack = onBack,
            actions = { ShareIconButton(ModelsDevLinks.lab(labId)) },
        )
        CatalogDivider()
        if (lab == null) {
            when {
                state.loading -> EmptyState("Loading lab…")
                state.syncState is SyncState.Failed ->
                    ErrorState((state.syncState as SyncState.Failed).message, viewModel::refresh)
                else -> EmptyState("Lab not found")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(MinGridCellWidth),
                modifier = Modifier.weight(1f),
                contentPadding = listPadding,
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column {
                        LabDetailHeader(lab)
                        SectionHeader("Models (${lab.modelCount})")
                        if (state.models.isEmpty()) EmptyState("No models listed")
                    }
                }
                items(state.models, key = { it.id }) { model ->
                    CanonicalModelRow(model) { onModelClick(model.id) }
                }
            }
        }
    }
}

@Composable
private fun LabDetailHeader(lab: Lab) {
    Column(
        Modifier.padding(Dimens.lg),
        verticalArrangement = Arrangement.spacedBy(Dimens.sm),
    ) {
        Text(lab.id, style = MonoStyle, color = LocalAppPalette.current.textTertiary)
        lab.description?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
    }
    FactGrid(
        listOf(
            Fact("Models", lab.modelCount.toString(), true),
            Fact("Providers", lab.providerCount.toString(), true),
            Fact("Updated", lab.lastUpdated),
        ),
        compactColumns = 2,
    )
}
