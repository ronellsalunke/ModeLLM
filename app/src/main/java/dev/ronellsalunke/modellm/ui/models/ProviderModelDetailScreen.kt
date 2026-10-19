package dev.ronellsalunke.modellm.ui.models

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ronellsalunke.modellm.data.repository.SyncState
import dev.ronellsalunke.modellm.ui.components.*
import dev.ronellsalunke.modellm.ui.theme.*
import dev.ronellsalunke.modellm.ui.util.*
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderModelDetailScreen(providerId: String, modelId: String, onBack: () -> Unit) {
    val viewModel = koinViewModel<ProviderModelDetailViewModel>(key = "$providerId/$modelId", parameters = { parametersOf(providerId, modelId) })
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val model = state.model
    val scrollBehavior = rememberDetailScrollBehavior()
    Column(Modifier.fillMaxSize().background(LocalAppPalette.current.background).nestedScroll(scrollBehavior.nestedScrollConnection)) {
        DetailTopAppBar(title = model?.name ?: "Model", onBack = onBack, scrollBehavior = scrollBehavior)
        CatalogDivider()
        if (model == null) {
            when {
                state.loading -> EmptyState("Loading model…")
                state.syncState is SyncState.Failed -> ErrorState((state.syncState as SyncState.Failed).message, viewModel::refresh)
                else -> EmptyState("Model not found", "This model is not in the saved catalog.")
            }
        } else {
            val facts = remember(model) {
                listOf(
                    Fact("Provider", model.providerName), Fact("Family", model.family),
                    Fact("Context", tokenLimit(model.limits.context), newRow = true),
                    Fact("Max input", tokenLimit(model.limits.input), newRow = true), Fact("Max output", tokenLimit(model.limits.output)),
                    Fact("Knowledge", model.knowledge, newRow = true),
                    Fact("Release", model.releaseDate, newRow = true), Fact("Updated", model.lastUpdated),
                    Fact("Weights", if (model.openWeights) "Open weights" else "Closed weights", newRow = true),
                    Fact("Type", model.modelType, newRow = true),
                    Fact("Canonical ID", model.canonicalModelId, mono = true, fullWidth = true),
                ) + listOfNotNull(model.providerNpm?.let { Fact("Model npm", it, mono = true, fullWidth = true) },
                    if (model.supportsInterleaved) Fact("Interleaved reasoning", "Supported", fullWidth = true) else null)
            }
            val costs = remember(model.cost) {
                listOf("Input" to model.cost.input, "Output" to model.cost.output,
                    "Cache read" to model.cost.cacheRead, "Cache write" to model.cost.cacheWrite,
                    "Reasoning" to model.cost.reasoning, "Audio input" to model.cost.inputAudio,
                    "Audio output" to model.cost.outputAudio)
            }
            val capabilities = remember(model) { model.capabilities() }
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = Dimens.xl)) {
                item {
                    Column(Modifier.padding(Dimens.lg), verticalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                        Text(model.id, style = MonoStyle, color = LocalAppPalette.current.textSecondary)
                        model.status?.let { StatusChip(it) }
                        if (model.experimental) StatusChip("Experimental")
                        model.description?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
                    }
                    FactGrid(facts, compactColumns = 2)
                    SectionHeader("Modalities")
                    Column(Modifier.padding(horizontal = Dimens.lg), verticalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                        Text("Input", style = MonoLabelStyle)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.xs), verticalArrangement = Arrangement.spacedBy(Dimens.xs)) {
                            model.inputModalities.forEach { ModalityTile(it) }
                            if (model.inputModalities.isEmpty()) Text("–", style = MonoStyle)
                        }
                        Text("Output", style = MonoLabelStyle)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.xs), verticalArrangement = Arrangement.spacedBy(Dimens.xs)) {
                            model.outputModalities.forEach { ModalityTile(it) }
                            if (model.outputModalities.isEmpty()) Text("–", style = MonoStyle)
                        }
                    }
                    SectionHeader("Pricing · per 1M tokens")
                    costs.forEach { (label, cost) ->
                        Row(Modifier.fillMaxWidth().padding(horizontal = Dimens.lg, vertical = Dimens.sm)) {
                            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            Text(price(cost), style = MonoStyle)
                        }
                        CatalogDivider()
                    }
                    SectionHeader("Capabilities")
                    FlowRow(Modifier.padding(horizontal = Dimens.lg),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.xs), verticalArrangement = Arrangement.spacedBy(Dimens.xs)) {
                        capabilities.forEach { CapabilityBadge(it) }
                        if (capabilities.isEmpty()) Text("None listed", style = MaterialTheme.typography.bodyMedium)
                    }
                    if (model.reasoningOptions.isNotEmpty()) {
                        SectionHeader("Reasoning options")
                        model.reasoningOptions.forEach { option ->
                            FactGrid(listOf(Fact("Type", option.type),
                                Fact("Values", option.values?.filterNotNull()?.joinToString(", ")),
                                Fact("Minimum", option.min?.let(::groupedNumber)), Fact("Maximum", option.max?.let(::groupedNumber))))
                        }
                    }
                    SectionHeader("Provider facts")
                    state.provider?.let { ProviderFacts(it) }
                }
            }
        }
    }
}
