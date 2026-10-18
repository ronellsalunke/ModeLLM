package dev.ronellsalunke.modellm.ui.providers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ronellsalunke.modellm.data.model.Model
import dev.ronellsalunke.modellm.data.repository.SyncState
import dev.ronellsalunke.modellm.ui.components.*
import dev.ronellsalunke.modellm.ui.theme.Dimens
import dev.ronellsalunke.modellm.ui.theme.LocalAppPalette
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ProviderDetailScreen(providerId: String, onBack: () -> Unit, onModelClick: (Model) -> Unit) {
    val viewModel = koinViewModel<ProviderDetailViewModel>(key = providerId, parameters = { parametersOf(providerId) })
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val provider = state.provider
    Column(Modifier.fillMaxSize().background(LocalAppPalette.current.background)) {
        BackHeader("Providers / ${provider?.name ?: providerId}", onBack)
        if (provider == null) {
            when {
                state.loading -> EmptyState("Loading provider…")
                state.syncState is SyncState.Failed -> ErrorState((state.syncState as SyncState.Failed).message, viewModel::refresh)
                else -> EmptyState("Provider not found", "This provider is not in the saved catalog.")
            }
        } else LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = Dimens.xl)) {
            item {
                ProviderFacts(provider)
                CatalogDivider()
                SectionHeader("Models (${provider.modelCount})")
                if (state.models.isEmpty()) EmptyState("No models listed")
            }
            items(state.models, key = { "${it.providerId}/${it.id}" }, contentType = { "model" }) { model ->
                ModelRow(model) { onModelClick(model) }
            }
        }
    }
}
