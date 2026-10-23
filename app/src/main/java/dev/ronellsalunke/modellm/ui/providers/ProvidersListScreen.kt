package dev.ronellsalunke.modellm.ui.providers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import dev.ronellsalunke.modellm.data.model.Provider
import dev.ronellsalunke.modellm.data.repository.SyncState
import dev.ronellsalunke.modellm.ui.components.AppPullToRefreshBox
import dev.ronellsalunke.modellm.ui.components.CatalogDivider
import dev.ronellsalunke.modellm.ui.components.CatalogRowMinHeight
import dev.ronellsalunke.modellm.ui.components.EmptyState
import dev.ronellsalunke.modellm.ui.components.ErrorState
import dev.ronellsalunke.modellm.ui.components.MinGridCellWidth
import dev.ronellsalunke.modellm.ui.components.navigationAreaBottomPadding
import dev.ronellsalunke.modellm.ui.components.HomeTopAppBar
import dev.ronellsalunke.modellm.ui.components.ProviderLogo
import dev.ronellsalunke.modellm.ui.components.SearchField
import dev.ronellsalunke.modellm.ui.components.SyncBanner
import dev.ronellsalunke.modellm.ui.components.catalogShortcuts
import dev.ronellsalunke.modellm.ui.components.navigationBarBottomPadding
import dev.ronellsalunke.modellm.ui.components.tabScreenPadding
import dev.ronellsalunke.modellm.ui.theme.Dimens
import dev.ronellsalunke.modellm.ui.theme.LocalAppPalette
import dev.ronellsalunke.modellm.ui.theme.MonoStyle
import dev.ronellsalunke.modellm.ui.util.groupedNumber
import org.koin.androidx.compose.koinViewModel

@Composable
fun ProvidersListScreen(
    onProviderClick: (String) -> Unit,
    viewModel: ProvidersViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    val providers =
        remember(state.providers, query) {
            state.providers.filter { it.name.contains(query, true) || it.id.contains(query, true) }
        }
    val searchFocusRequester = remember { FocusRequester() }
    Column(
        Modifier.tabScreenPadding().background(LocalAppPalette.current.background)
            .catalogShortcuts(searchFocusRequester, viewModel::refresh),
    ) {
        HomeTopAppBar(title = "Providers")
        CatalogDivider()
        SearchField(
            query,
            { query = it },
            "Search providers",
            Modifier.fillMaxWidth().padding(Dimens.sm),
            focusRequester = searchFocusRequester,
        )
        SyncBanner(state.lastSyncedAt, state.syncState, viewModel::refresh)
        AppPullToRefreshBox(
            isRefreshing = state.syncState is SyncState.Running,
            onRefresh = viewModel::refresh,
            modifier = Modifier.weight(1f),
        ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(MinGridCellWidth),
                modifier = Modifier.fillMaxSize(),
                contentPadding =
                    PaddingValues(
                        bottom = navigationAreaBottomPadding() + navigationBarBottomPadding(),
                    ),
            ) {
                if (providers.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        ProvidersEmptyState(
                            query = query,
                            syncState = state.syncState,
                            loading = state.loading,
                            lastSyncedAt = state.lastSyncedAt,
                            onClearQuery = { query = "" },
                            onRetry = viewModel::refresh,
                        )
                    }
                }
                items(providers, key = { it.id }) { provider ->
                    ProviderRow(provider = provider, onProviderClick = onProviderClick)
                }
            }
        }
    }
}

@Composable
private fun ProvidersEmptyState(
    query: String,
    syncState: SyncState,
    loading: Boolean,
    lastSyncedAt: Long?,
    onClearQuery: () -> Unit,
    onRetry: () -> Unit,
) {
    val failure = syncState as? SyncState.Failed
    when {
        query.isNotBlank() ->
            EmptyState(
                "No providers match",
                actionLabel = "Clear search",
                onAction = onClearQuery,
            )
        failure != null -> ErrorState(failure.message, onRetry)
        loading || lastSyncedAt == null ->
            EmptyState("Downloading the model catalogue…")
        else ->
            EmptyState(
                "No providers in the catalog",
                actionLabel = "Retry",
                onAction = onRetry,
            )
    }
}

@Composable
private fun ProviderRow(provider: Provider, onProviderClick: (String) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth()
                .heightIn(min = CatalogRowMinHeight)
                .clickable { onProviderClick(provider.id) }
                .padding(Dimens.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.md),
        ) {
            ProviderLogo(provider.id, provider.name)
            Column(Modifier.weight(1f)) {
                Text(
                    provider.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                provider.npm?.let {
                    Text(
                        it,
                        style = MonoStyle,
                        color = LocalAppPalette.current.textTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Text(groupedNumber(provider.modelCount), style = MonoStyle)
        }
        CatalogDivider()
    }
}
