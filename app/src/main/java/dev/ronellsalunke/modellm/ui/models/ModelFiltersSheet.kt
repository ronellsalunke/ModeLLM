package dev.ronellsalunke.modellm.ui.models

import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.ronellsalunke.modellm.data.model.*
import dev.ronellsalunke.modellm.ui.components.*
import dev.ronellsalunke.modellm.ui.theme.Dimens
import dev.ronellsalunke.modellm.ui.theme.LocalAppPalette
import dev.ronellsalunke.modellm.ui.util.capabilityLabel
import dev.ronellsalunke.modellm.ui.util.modalityLabel
import kotlinx.coroutines.launch

data class ActiveFilter(val key: String, val label: String, val remove: ModelFilters)

fun ModelFilters.activeFilters(providers: List<Provider>, labs: List<Lab>): List<ActiveFilter> = buildList {
    labIds.forEach { id -> add(ActiveFilter("lab:$id", labs.firstOrNull { it.id == id }?.name ?: id,
        copy(labIds = labIds - id))) }
    providerIds.forEach { id -> add(ActiveFilter("provider:$id", providers.firstOrNull { it.id == id }?.name ?: id,
        copy(providerIds = providerIds - id))) }
    inputModalities.forEach { add(ActiveFilter("input:$it", "Input: ${modalityLabel(it)}", copy(inputModalities = inputModalities - it))) }
    capabilities.forEach { add(ActiveFilter("capability:$it", capabilityLabel(it), copy(capabilities = capabilities - it))) }
    if (openWeights == true) add(ActiveFilter("weights", "Open weights", copy(openWeights = null)))
    knowledgeAfter?.let { add(ActiveFilter("knowledge", "Knowledge: ${it.take(4)}+", copy(knowledgeAfter = null))) }
}

private fun <T> Set<T>.toggle(value: T): Set<T> = if (value in this) this - value else this + value

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelFiltersSheet(
    filters: ModelFilters,
    providers: List<Provider>,
    labs: List<Lab>,
    onDismiss: () -> Unit,
    onUpdateFilters: ((ModelFilters) -> ModelFilters) -> Unit,
) {
    var providerQuery by remember { mutableStateOf("") }
    var labQuery by remember { mutableStateOf("") }
    var showLabs by remember { mutableStateOf(true) }
    val visibleLabs = remember(labs, labQuery) { labs.filter { it.name.contains(labQuery, true) || it.id.contains(labQuery, true) } }
    val visibleProviders = remember(providers, providerQuery) {
        providers.filter { it.name.contains(providerQuery, true) || it.id.contains(providerQuery, true) }
    }
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    )
    val scope = rememberCoroutineScope()
    val hideSheet: () -> Unit = {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) onDismiss()
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = LocalAppPalette.current.background,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
    ) {
        val focusManager = LocalFocusManager.current
        val keyboardController = LocalSoftwareKeyboardController.current
        DisposableEffect(focusManager, keyboardController) {
            onDispose { focusManager.clearFocus(); keyboardController?.hide() }
        }
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.9f).imePadding()) {
            Text("Filters", style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = Dimens.lg, vertical = Dimens.sm))
            LazyColumn(Modifier.weight(1f)) {
                item {
                    SectionHeader("Input modalities · match all")
                    FlowRow(Modifier.padding(horizontal = Dimens.lg), horizontalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                        Modality.entries.forEach { modality ->
                            FilterChip(selected = modality in filters.inputModalities, shape = MaterialTheme.shapes.small,
                                onClick = { onUpdateFilters { it.copy(inputModalities = it.inputModalities.toggle(modality)) } },
                                label = { Text(modalityLabel(modality)) })
                        }
                    }
                    SectionHeader("Capabilities · match all")
                    FlowRow(Modifier.padding(horizontal = Dimens.lg), horizontalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                        Capability.entries.forEach { capability ->
                            FilterChip(selected = capability in filters.capabilities, shape = MaterialTheme.shapes.small,
                                onClick = { onUpdateFilters { it.copy(capabilities = it.capabilities.toggle(capability)) } },
                                label = { Text(capabilityLabel(capability)) })
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(horizontal = Dimens.lg), verticalAlignment = Alignment.CenterVertically) {
                        Text("Open weights only", modifier = Modifier.weight(1f))
                        Switch(checked = filters.openWeights == true, onCheckedChange = { checked -> onUpdateFilters { it.copy(openWeights = if (checked) true else null) } },
                            modifier = Modifier.semantics { contentDescription = "Open weights only" })
                    }
                    SectionHeader("Knowledge cutoff")
                    FlowRow(Modifier.padding(horizontal = Dimens.lg), horizontalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                        listOf("2024", "2025", "2026").forEach { year ->
                            val cutoff = "$year-01"
                            FilterChip(selected = filters.knowledgeAfter == cutoff, shape = MaterialTheme.shapes.small,
                                onClick = { onUpdateFilters { it.copy(knowledgeAfter = if (it.knowledgeAfter == cutoff) null else cutoff) } },
                                label = { Text("$year+") })
                        }
                    }
                    SingleChoiceButtonGroup(
                        labels = listOf("Labs (${labs.size})", "Providers (${providers.size})"),
                        selectedIndex = if (showLabs) 0 else 1,
                        onSelect = { showLabs = it == 0 },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.lg, vertical = Dimens.sm),
                    )
                    SearchField(if (showLabs) labQuery else providerQuery,
                        { if (showLabs) labQuery = it else providerQuery = it },
                        if (showLabs) "Search labs" else "Search providers",
                        Modifier.fillMaxWidth().padding(horizontal = Dimens.lg, vertical = Dimens.sm))
                }
                if (showLabs) {
                    items(visibleLabs, key = { "lab:${it.id}" }) { lab ->
                        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .toggleable(value = lab.id in filters.labIds, role = Role.Checkbox,
                                onValueChange = { onUpdateFilters { it.copy(labIds = it.labIds.toggle(lab.id)) } })
                            .padding(horizontal = Dimens.lg), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                            Checkbox(checked = lab.id in filters.labIds, onCheckedChange = null)
                            LabLogo(lab.id, lab.name, 20.dp)
                            Text(lab.name, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                } else {
                    items(visibleProviders, key = { "provider:${it.id}" }) { provider ->
                        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .toggleable(value = provider.id in filters.providerIds, role = Role.Checkbox,
                                onValueChange = { onUpdateFilters { it.copy(providerIds = it.providerIds.toggle(provider.id)) } })
                            .padding(horizontal = Dimens.lg), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                            Checkbox(checked = provider.id in filters.providerIds, onCheckedChange = null)
                            ProviderLogo(provider.id, provider.name, 20.dp)
                            Text(provider.name, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            CatalogDivider()
            Row(Modifier.fillMaxWidth().padding(Dimens.lg), horizontalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                OutlinedButton(onClick = { onUpdateFilters { ModelFilters() }; providerQuery = ""; labQuery = "" }, modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LocalAppPalette.current.brandText),
                    border = BorderStroke(1.dp, LocalAppPalette.current.border)) { Text("Reset") }
                Button(onClick = hideSheet, modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.small) { Text("Done") }
            }
        }
    }
}
