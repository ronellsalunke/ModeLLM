package dev.ronellsalunke.modellm.ui.models

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ronellsalunke.modellm.data.model.Capability
import dev.ronellsalunke.modellm.data.model.Modality
import dev.ronellsalunke.modellm.data.model.Model
import dev.ronellsalunke.modellm.data.model.ModelCost
import dev.ronellsalunke.modellm.data.model.Provider
import dev.ronellsalunke.modellm.data.model.ReasoningOption
import dev.ronellsalunke.modellm.data.repository.SyncState
import dev.ronellsalunke.modellm.ui.components.CapabilityBadge
import dev.ronellsalunke.modellm.ui.components.CatalogDivider
import dev.ronellsalunke.modellm.ui.components.DetailTopAppBar
import dev.ronellsalunke.modellm.ui.components.EmptyState
import dev.ronellsalunke.modellm.ui.components.ErrorState
import dev.ronellsalunke.modellm.ui.components.Fact
import dev.ronellsalunke.modellm.ui.components.FactGrid
import dev.ronellsalunke.modellm.ui.components.ModalityTile
import dev.ronellsalunke.modellm.ui.components.ProviderFacts
import dev.ronellsalunke.modellm.ui.components.SectionHeader
import dev.ronellsalunke.modellm.ui.components.ShareIconButton
import dev.ronellsalunke.modellm.ui.components.StatusChip
import dev.ronellsalunke.modellm.ui.components.capabilities
import dev.ronellsalunke.modellm.ui.components.rememberDetailScrollBehavior
import dev.ronellsalunke.modellm.ui.components.safeDrawingBottomPadding
import dev.ronellsalunke.modellm.ui.components.safeDrawingHorizontalPadding
import dev.ronellsalunke.modellm.ui.theme.Dimens
import dev.ronellsalunke.modellm.ui.theme.LocalAppPalette
import dev.ronellsalunke.modellm.ui.theme.MonoLabelStyle
import dev.ronellsalunke.modellm.ui.theme.MonoStyle
import dev.ronellsalunke.modellm.ui.util.ModelsDevLinks
import dev.ronellsalunke.modellm.ui.util.groupedNumber
import dev.ronellsalunke.modellm.ui.util.price
import dev.ronellsalunke.modellm.ui.util.tokenLimit
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

private const val CompactGridColumns = 2

@Composable
fun ProviderModelDetailScreen(providerId: String, modelId: String, onBack: () -> Unit) {
    val viewModel =
        koinViewModel<ProviderModelDetailViewModel>(
            key = "$providerId/$modelId",
            parameters = { parametersOf(providerId, modelId) },
        )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val model = state.model
    val shareUrl =
        model?.let { loaded ->
            loaded.canonicalModelId?.let { ModelsDevLinks.model(it) }
                ?: ModelsDevLinks.provider(providerId)
        }
    val scrollBehavior = rememberDetailScrollBehavior()
    Column(
        Modifier.fillMaxSize()
            .background(LocalAppPalette.current.background)
            .safeDrawingHorizontalPadding()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
    ) {
        DetailTopAppBar(
            title = model?.name ?: "Model",
            onBack = onBack,
            scrollBehavior = scrollBehavior,
            actions = { shareUrl?.let { ShareIconButton(it) } },
        )
        CatalogDivider()
        if (model == null) {
            ProviderModelEmptyState(state = state, onRefresh = viewModel::refresh)
        } else {
            ProviderModelDetailContent(
                model = model,
                provider = state.provider,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ProviderModelEmptyState(
    state: ProviderModelDetailUiState,
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
private fun ProviderModelDetailContent(
    model: Model,
    provider: Provider?,
    modifier: Modifier = Modifier,
) {
    val facts = remember(model) { providerModelFacts(model) }
    val capabilities = remember(model) { model.capabilities() }
    LazyColumn(
        modifier,
        contentPadding = PaddingValues(bottom = Dimens.xl + safeDrawingBottomPadding()),
    ) {
        item {
            ProviderModelHeader(model)
            FactGrid(facts, compactColumns = CompactGridColumns)
            ModelModalitiesSection(model.inputModalities, model.outputModalities)
            ModelPricingSection(model.cost)
            ModelCapabilitiesSection(capabilities)
            ModelReasoningSection(model.reasoningOptions)
            SectionHeader("Provider facts")
            provider?.let { ProviderFacts(it) }
        }
    }
}

@Composable
private fun ProviderModelHeader(model: Model) {
    Column(
        Modifier.padding(Dimens.lg),
        verticalArrangement = Arrangement.spacedBy(Dimens.sm),
    ) {
        Text(
            model.id,
            style = MonoStyle,
            color = LocalAppPalette.current.textSecondary,
        )
        model.status?.let { StatusChip(it) }
        if (model.experimental) {
            StatusChip("Experimental")
        }
        model.description
            ?.takeIf { it.isNotBlank() }
            ?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
    }
}

private fun providerModelFacts(model: Model): List<Fact> =
    listOf(
        Fact("Provider", model.providerName),
        Fact("Family", model.family),
        Fact("Context", tokenLimit(model.limits.context), newRow = true),
        Fact("Max input", tokenLimit(model.limits.input), newRow = true),
        Fact("Max output", tokenLimit(model.limits.output)),
        Fact("Knowledge", model.knowledge, newRow = true),
        Fact("Release", model.releaseDate, newRow = true),
        Fact("Updated", model.lastUpdated),
        Fact(
            "Weights",
            if (model.openWeights) "Open weights" else "Closed weights",
            newRow = true,
        ),
        Fact("Type", model.modelType, newRow = true),
        Fact("Canonical ID", model.canonicalModelId, mono = true, fullWidth = true),
    ) +
        listOfNotNull(
            model.providerNpm?.let {
                Fact("Model npm", it, mono = true, fullWidth = true)
            },
            if (model.supportsInterleaved) {
                Fact("Interleaved reasoning", "Supported", fullWidth = true)
            } else null,
        )

@Composable
private fun ModelModalitiesSection(
    inputModalities: List<Modality>,
    outputModalities: List<Modality>,
) {
    SectionHeader("Modalities")
    Column(
        Modifier.padding(horizontal = Dimens.lg),
        verticalArrangement = Arrangement.spacedBy(Dimens.sm),
    ) {
        Text("Input", style = MonoLabelStyle)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Dimens.xs),
            verticalArrangement = Arrangement.spacedBy(Dimens.xs),
        ) {
            inputModalities.forEach { ModalityTile(it) }
            if (inputModalities.isEmpty()) Text("–", style = MonoStyle)
        }
        Text("Output", style = MonoLabelStyle)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Dimens.xs),
            verticalArrangement = Arrangement.spacedBy(Dimens.xs),
        ) {
            outputModalities.forEach { ModalityTile(it) }
            if (outputModalities.isEmpty()) Text("–", style = MonoStyle)
        }
    }
}

@Composable
private fun ModelPricingSection(cost: ModelCost) {
    val costs = remember(cost) {
        listOf(
            "Input" to cost.input,
            "Output" to cost.output,
            "Cache read" to cost.cacheRead,
            "Cache write" to cost.cacheWrite,
            "Reasoning" to cost.reasoning,
            "Audio input" to cost.inputAudio,
            "Audio output" to cost.outputAudio,
        )
    }
    SectionHeader("Pricing · per 1M tokens")
    costs.forEach { (label, priceValue) ->
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Dimens.lg, vertical = Dimens.sm),
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Text(price(priceValue), style = MonoStyle)
        }
        CatalogDivider()
    }
}

@Composable
private fun ModelCapabilitiesSection(capabilities: List<Capability>) {
    SectionHeader("Capabilities")
    FlowRow(
        Modifier.padding(horizontal = Dimens.lg),
        horizontalArrangement = Arrangement.spacedBy(Dimens.xs),
        verticalArrangement = Arrangement.spacedBy(Dimens.xs),
    ) {
        capabilities.forEach { CapabilityBadge(it) }
        if (capabilities.isEmpty()) {
            Text("None listed", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ModelReasoningSection(reasoningOptions: List<ReasoningOption>) {
    if (reasoningOptions.isEmpty()) return
    SectionHeader("Reasoning options")
    reasoningOptions.forEach { option ->
        FactGrid(
            listOf(
                Fact("Type", option.type),
                Fact("Values", option.values?.filterNotNull()?.joinToString(", ")),
                Fact("Minimum", option.min?.let(::groupedNumber)),
                Fact("Maximum", option.max?.let(::groupedNumber)),
            )
        )
    }
}
