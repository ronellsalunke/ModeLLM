package dev.ronellsalunke.modellm.ui.providers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ronellsalunke.modellm.data.repository.SyncState
import dev.ronellsalunke.modellm.ui.components.*
import dev.ronellsalunke.modellm.ui.theme.*
import dev.ronellsalunke.modellm.ui.util.groupedNumber
import dev.ronellsalunke.modellm.ui.util.pluralized
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProvidersListScreen(onProviderClick: (String) -> Unit, viewModel: ProvidersViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    val providers = remember(state.providers, query) {
        state.providers.filter { it.name.contains(query, true) || it.id.contains(query, true) }
    }
    Column(Modifier.fillMaxSize()) {
        AppHeader("PROVIDERS", pluralized(state.providers.size, "provider"))
        SearchField(query, { query = it }, "Search providers", Modifier.fillMaxWidth().padding(Dimens.sm))
        SyncBanner(state.lastSyncedAt, state.syncState, viewModel::refresh)
        PullToRefreshBox(isRefreshing = state.syncState is SyncState.Running, onRefresh = viewModel::refresh, modifier = Modifier.weight(1f)) {
            LazyColumn(Modifier.fillMaxSize()) {
                if (providers.isEmpty()) item {
                    val failure = state.syncState as? SyncState.Failed
                    when {
                        query.isNotBlank() -> EmptyState("No providers match", actionLabel = "Clear search", onAction = { query = "" })
                        failure != null -> ErrorState(failure.message, viewModel::refresh)
                        state.loading || state.lastSyncedAt == null -> EmptyState("Downloading the model catalogue…")
                        else -> EmptyState("No providers in the catalog", actionLabel = "Retry", onAction = viewModel::refresh)
                    }
                }
                items(providers, key = { it.id }) { provider ->
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { onProviderClick(provider.id) }.padding(Dimens.lg),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.md)) {
                        ProviderLogo(provider.id, provider.name)
                        Column(Modifier.weight(1f)) {
                            Text(provider.name, style = MaterialTheme.typography.titleSmall)
                            provider.npm?.let { Text(it, style = MonoStyle, color = LocalAppPalette.current.textTertiary,
                                maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        }
                        Text(groupedNumber(provider.modelCount), style = MonoStyle)
                    }
                    CatalogDivider()
                }
            }
        }
    }
}
