package dev.ronellsalunke.modellm.ui.models

import android.content.ClipData
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ronellsalunke.modellm.data.model.CanonicalModel
import dev.ronellsalunke.modellm.data.model.Model
import dev.ronellsalunke.modellm.data.repository.SyncState
import dev.ronellsalunke.modellm.ui.components.CanonicalProviderRow
import dev.ronellsalunke.modellm.ui.components.CatalogDivider
import dev.ronellsalunke.modellm.ui.components.DetailTopAppBar
import dev.ronellsalunke.modellm.ui.components.EmptyState
import dev.ronellsalunke.modellm.ui.components.ErrorState
import dev.ronellsalunke.modellm.ui.components.Fact
import dev.ronellsalunke.modellm.ui.components.FactGrid
import dev.ronellsalunke.modellm.ui.components.MinGridCellWidth
import dev.ronellsalunke.modellm.ui.components.navigationAreaBottomPadding
import dev.ronellsalunke.modellm.ui.components.SectionHeader
import dev.ronellsalunke.modellm.ui.components.ShareIconButton
import dev.ronellsalunke.modellm.ui.components.capabilities
import dev.ronellsalunke.modellm.ui.components.safeDrawingBottomPadding
import dev.ronellsalunke.modellm.ui.components.safeDrawingHorizontalPadding
import dev.ronellsalunke.modellm.ui.theme.Dimens
import dev.ronellsalunke.modellm.ui.theme.LocalAppPalette
import dev.ronellsalunke.modellm.ui.theme.MonoStyle
import dev.ronellsalunke.modellm.ui.util.ModelsDevLinks
import dev.ronellsalunke.modellm.ui.util.capabilityLabel
import dev.ronellsalunke.modellm.ui.util.modalityLabel
import dev.ronellsalunke.modellm.ui.util.tokenLimit
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

private const val CompactGridColumns = 2
private val CopyIconSize = 16.dp

@Composable
fun CanonicalModelDetailScreen(
    canonicalId: String,
    onBack: () -> Unit,
    onProviderClick: (String) -> Unit,
) {
    val viewModel =
        koinViewModel<CanonicalModelDetailViewModel>(
            key = canonicalId,
            parameters = { parametersOf(canonicalId) },
        )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val model = state.model
    Column(
        Modifier.fillMaxSize()
            .background(LocalAppPalette.current.background)
            .safeDrawingHorizontalPadding()
    ) {
        DetailTopAppBar(
            title = model?.name ?: "Model",
            onBack = onBack,
            actions = { ShareIconButton(ModelsDevLinks.model(canonicalId)) },
        )
        CatalogDivider()
        if (model == null) {
            CanonicalModelEmptyState(state = state, onRefresh = viewModel::refresh)
        } else {
            CanonicalModelDetailContent(
                model = model,
                providers = state.providers,
                onProviderClick = onProviderClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun CanonicalModelEmptyState(
    state: CanonicalModelDetailUiState,
    onRefresh: () -> Unit,
) {
    val syncFailure = state.syncState as? SyncState.Failed
    when {
        state.loading -> EmptyState("Loading model…")
        syncFailure != null -> ErrorState(syncFailure.message, onRefresh)
        else -> EmptyState("Model not found", "This model is not in the saved catalog.")
    }
}

@Composable
private fun CanonicalModelDetailContent(
    model: CanonicalModel,
    providers: List<Model>,
    onProviderClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val facts = remember(model) { canonicalModelFacts(model) }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(MinGridCellWidth),
        modifier = modifier,
        contentPadding = PaddingValues(
            bottom = Dimens.xl + safeDrawingBottomPadding() + navigationAreaBottomPadding(),
        ),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                CanonicalModelHeader(model)
                FactGrid(facts, compactColumns = CompactGridColumns)
                SectionHeader("Providers (${model.providerCount})")
                if (providers.isEmpty()) EmptyState("No providers listed")
            }
        }
        items(providers, key = { "${it.providerId}/${it.id}" }) { provider ->
            CanonicalProviderRow(provider, model.labName) {
                onProviderClick(provider.providerId)
            }
        }
    }
}

@Composable
private fun CanonicalModelHeader(model: CanonicalModel) {
    Column(
        Modifier.padding(Dimens.lg),
        verticalArrangement = Arrangement.spacedBy(Dimens.sm),
    ) {
        model.description
            ?.takeIf { it.isNotBlank() }
            ?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
        CanonicalModelIdRow(model.id)
    }
}

@Composable
private fun CanonicalModelIdRow(modelId: String) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modelId,
            style = MonoStyle,
            modifier = Modifier.weight(1f, fill = false),
        )
        IconButton(
            onClick = {
                scope.launch {
                    clipboard.setClipEntry(
                        ClipEntry(ClipData.newPlainText("Canonical model ID", modelId))
                    )
                }
            },
            modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
        ) {
            Icon(
                Icons.Default.ContentCopy,
                contentDescription = "Copy canonical model ID",
                modifier = Modifier.size(CopyIconSize),
            )
        }
    }
}

private fun canonicalModelFacts(model: CanonicalModel): List<Fact> =
    listOf(
        Fact("Lab", model.labName),
        Fact("Family", model.family, newRow = true),
        Fact("Providers", model.providerCount.toString(), mono = true),
        Fact("Context", tokenLimit(model.limits.context), mono = true, newRow = true),
        Fact("Output limit", tokenLimit(model.limits.output), mono = true),
        Fact("Knowledge", model.knowledge, newRow = true),
        Fact("Release", model.releaseDate, newRow = true),
        Fact("Updated", model.lastUpdated),
        Fact(
            "Weights",
            if (model.openWeights) "Open weights" else "Closed weights",
            newRow = true,
        ),
        Fact(
            "Input modalities",
            model.inputModalities.joinToString { modalityLabel(it) }.ifEmpty { "–" },
            newRow = true,
        ),
        Fact(
            "Output modalities",
            model.outputModalities.joinToString { modalityLabel(it) }.ifEmpty { "–" },
            newRow = true,
        ),
        Fact(
            "Capabilities",
            model.capabilities().joinToString { capabilityLabel(it) }.ifEmpty { "None listed" },
            newRow = true,
        ),
    )
