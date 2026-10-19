package dev.ronellsalunke.modellm.ui.models

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import android.content.ClipData
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ronellsalunke.modellm.data.repository.SyncState
import dev.ronellsalunke.modellm.ui.components.*
import dev.ronellsalunke.modellm.ui.theme.*
import dev.ronellsalunke.modellm.ui.util.*
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanonicalModelDetailScreen(canonicalId: String, onBack: () -> Unit, onProviderClick: (String) -> Unit) {
    val viewModel = koinViewModel<CanonicalModelDetailViewModel>(key = canonicalId, parameters = { parametersOf(canonicalId) })
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val model = state.model
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
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
        } else LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = Dimens.xl)) {
            item {
                Column(Modifier.padding(Dimens.lg), verticalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                    model.description?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {
                        Text(model.id, style = MonoStyle, modifier = Modifier.weight(1f, fill = false))
                        IconButton(onClick = { scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("Canonical model ID", model.id))) } }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.ContentCopy, "Copy canonical model ID", modifier = Modifier.size(16.dp))
                        }
                    }
                }
                FactGrid(listOf(
                    Fact("Lab", model.labName),
                    Fact("Family", model.family, newRow = true), Fact("Providers", model.providerCount.toString(), mono = true),
                    Fact("Context", tokenLimit(model.limits.context), mono = true, newRow = true),
                    Fact("Output limit", tokenLimit(model.limits.output), mono = true),
                    Fact("Knowledge", model.knowledge, newRow = true),
                    Fact("Release", model.releaseDate, newRow = true), Fact("Updated", model.lastUpdated),
                    Fact("Weights", if (model.openWeights) "Open weights" else "Closed weights", newRow = true),
                    Fact("Input modalities", model.inputModalities.joinToString { modalityLabel(it) }.ifEmpty { "–" }, newRow = true),
                    Fact("Output modalities", model.outputModalities.joinToString { modalityLabel(it) }.ifEmpty { "–" }, newRow = true),
                    Fact("Capabilities", model.capabilities().joinToString { capabilityLabel(it) }.ifEmpty { "None listed" }, newRow = true),
                ), compactColumns = 2)
                SectionHeader("Providers (${model.providerCount})")
                if (state.providers.isEmpty()) EmptyState("No providers listed")
            }
            items(state.providers, key = { "${it.providerId}/${it.id}" }) { provider ->
                CanonicalProviderRow(provider, model.labName) { onProviderClick(provider.providerId) }
            }
        }
    }
}
