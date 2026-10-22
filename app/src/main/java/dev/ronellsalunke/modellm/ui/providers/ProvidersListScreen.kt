package dev.ronellsalunke.modellm.ui.providers

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ronellsalunke.modellm.data.model.Provider
import dev.ronellsalunke.modellm.data.repository.SyncState
import dev.ronellsalunke.modellm.ui.components.AppPullToRefreshBox
import dev.ronellsalunke.modellm.ui.components.CatalogDivider
import dev.ronellsalunke.modellm.ui.components.EmptyState
import dev.ronellsalunke.modellm.ui.components.ErrorState
import dev.ronellsalunke.modellm.ui.components.FloatingTabBarClearance
import dev.ronellsalunke.modellm.ui.components.HomeTopAppBar
import dev.ronellsalunke.modellm.ui.components.ProviderLogo
import dev.ronellsalunke.modellm.ui.components.SearchField
import dev.ronellsalunke.modellm.ui.components.SyncBanner
import dev.ronellsalunke.modellm.ui.components.navigationBarBottomPadding
import dev.ronellsalunke.modellm.ui.components.tabScreenPadding
import dev.ronellsalunke.modellm.ui.theme.Dimens
import dev.ronellsalunke.modellm.ui.theme.LocalAppPalette
import dev.ronellsalunke.modellm.ui.theme.MonoStyle
import dev.ronellsalunke.modellm.ui.util.groupedNumber
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
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
    Column(Modifier.tabScreenPadding().background(LocalAppPalette.current.background)) {
        HomeTopAppBar(title = "Providers")
        CatalogDivider()
        SearchField(
            query,
            { query = it },
            "Search providers",
            Modifier.fillMaxWidth().padding(Dimens.sm),
        )
        SyncBanner(state.lastSyncedAt, state.syncState, viewModel::refresh)
        AppPullToRefreshBox(
            isRefreshing = state.syncState is SyncState.Running,
            onRefresh = viewModel::refresh,
            modifier = Modifier.weight(1f),
        ) {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding =
                    PaddingValues(bottom = FloatingTabBarClearance + navigationBarBottomPadding()),
            ) {
                if (providers.isEmpty()) {
                    item {
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
    Row(
        Modifier.fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable { onProviderClick(provider.id) }
            .padding(Dimens.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.md),
    ) {
        ProviderLogo(provider.id, provider.name)
        Column(Modifier.weight(1f)) {
            Text(provider.name, style = MaterialTheme.typography.titleSmall)
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
