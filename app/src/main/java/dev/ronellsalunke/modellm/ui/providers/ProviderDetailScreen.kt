package dev.ronellsalunke.modellm.ui.providers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ronellsalunke.modellm.data.model.Model
import dev.ronellsalunke.modellm.data.repository.SyncState
import dev.ronellsalunke.modellm.ui.components.CatalogDivider
import dev.ronellsalunke.modellm.ui.components.DetailTopAppBar
import dev.ronellsalunke.modellm.ui.components.EmptyState
import dev.ronellsalunke.modellm.ui.components.ErrorState
import dev.ronellsalunke.modellm.ui.components.ModelRow
import dev.ronellsalunke.modellm.ui.components.MinGridCellWidth
import dev.ronellsalunke.modellm.ui.components.navigationAreaBottomPadding
import dev.ronellsalunke.modellm.ui.components.ProviderFacts
import dev.ronellsalunke.modellm.ui.components.SectionHeader
import dev.ronellsalunke.modellm.ui.components.ShareIconButton
import dev.ronellsalunke.modellm.ui.components.rememberDetailScrollBehavior
import dev.ronellsalunke.modellm.ui.components.safeDrawingBottomPadding
import dev.ronellsalunke.modellm.ui.components.safeDrawingHorizontalPadding
import dev.ronellsalunke.modellm.ui.theme.Dimens
import dev.ronellsalunke.modellm.ui.theme.LocalAppPalette
import dev.ronellsalunke.modellm.ui.util.ModelsDevLinks
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ProviderDetailScreen(providerId: String, onBack: () -> Unit, onModelClick: (Model) -> Unit) {
    val viewModel =
        koinViewModel<ProviderDetailViewModel>(
            key = providerId,
            parameters = { parametersOf(providerId) },
        )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val provider = state.provider
    val scrollBehavior = rememberDetailScrollBehavior()
    Column(
        Modifier.fillMaxSize()
            .background(LocalAppPalette.current.background)
            .safeDrawingHorizontalPadding()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
    ) {
        DetailTopAppBar(
            title = provider?.name ?: providerId,
            onBack = onBack,
            scrollBehavior = scrollBehavior,
            actions = { ShareIconButton(ModelsDevLinks.provider(providerId)) },
        )
        CatalogDivider()
        if (provider == null) {
            when {
                state.loading -> EmptyState("Loading provider…")
                state.syncState is SyncState.Failed ->
                    ErrorState((state.syncState as SyncState.Failed).message, viewModel::refresh)
                else ->
                    EmptyState("Provider not found", "This provider is not in the saved catalog.")
            }
        } else
            LazyVerticalGrid(
                columns = GridCells.Adaptive(MinGridCellWidth),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(
                    bottom = Dimens.xl + safeDrawingBottomPadding() + navigationAreaBottomPadding(),
                ),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column {
                        Spacer(Modifier.height(Dimens.lg))
                        ProviderFacts(provider, showIdentity = false)
                        CatalogDivider()
                        SectionHeader("Models (${provider.modelCount})")
                        if (state.models.isEmpty()) EmptyState("No models listed")
                    }
                }
                items(
                    state.models,
                    key = { "${it.providerId}/${it.id}" },
                    contentType = { "model" },
                ) { model ->
                    ModelRow(model) { onModelClick(model) }
                }
            }
    }
}
