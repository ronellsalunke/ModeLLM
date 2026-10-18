package dev.ronellsalunke.modellm.ui.models

import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.ronellsalunke.modellm.data.model.*
import dev.ronellsalunke.modellm.ui.components.*
import dev.ronellsalunke.modellm.ui.theme.Dimens
import dev.ronellsalunke.modellm.ui.theme.LocalAppPalette
import dev.ronellsalunke.modellm.ui.util.capabilityLabel
import dev.ronellsalunke.modellm.ui.util.modalityLabel

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
    onApply: (ModelFilters) -> Unit,
) {
    var draft by remember { mutableStateOf(filters) }
    var providerQuery by remember { mutableStateOf("") }
    var labQuery by remember { mutableStateOf("") }
    var showLabs by remember { mutableStateOf(true) }
    val visibleLabs = remember(labs, labQuery) { labs.filter { it.name.contains(labQuery, true) || it.id.contains(labQuery, true) } }
    val visibleProviders = remember(providers, providerQuery) {
        providers.filter { it.name.contains(providerQuery, true) || it.id.contains(providerQuery, true) }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        val focusManager = LocalFocusManager.current
        val keyboardController = LocalSoftwareKeyboardController.current
        val clearInput = {
            focusManager.clearFocus()
            keyboardController?.hide()
            Unit
        }
        DisposableEffect(focusManager, keyboardController) {
            onDispose { clearInput() }
        }
        val dismiss = { clearInput(); onDismiss() }
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f)).clickable(onClick = dismiss))
            Surface(Modifier.align(Alignment.BottomCenter).fillMaxWidth().imePadding().navigationBarsPadding().statusBarsPadding().fillMaxHeight(0.9f),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp), color = LocalAppPalette.current.background) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(start = Dimens.lg, end = Dimens.sm), verticalAlignment = Alignment.CenterVertically) {
                Text("Filter models", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = dismiss) { Icon(Icons.Default.Close, contentDescription = "Close filters") }
            }
            LazyColumn(Modifier.weight(1f)) {
                item {
                    SectionHeader("Input modalities · match all")
                    FlowRow(Modifier.padding(horizontal = Dimens.lg), horizontalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                        Modality.entries.forEach { modality ->
                            FilterChip(selected = modality in draft.inputModalities, shape = MaterialTheme.shapes.small,
                                onClick = { draft = draft.copy(inputModalities = draft.inputModalities.toggle(modality)) },
                                label = { Text(modalityLabel(modality)) })
                        }
                    }
                    SectionHeader("Capabilities · match all")
                    FlowRow(Modifier.padding(horizontal = Dimens.lg), horizontalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                        Capability.entries.forEach { capability ->
                            FilterChip(selected = capability in draft.capabilities, shape = MaterialTheme.shapes.small,
                                onClick = { draft = draft.copy(capabilities = draft.capabilities.toggle(capability)) },
                                label = { Text(capabilityLabel(capability)) })
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(horizontal = Dimens.lg), verticalAlignment = Alignment.CenterVertically) {
                        Text("Open weights only", modifier = Modifier.weight(1f))
                        Switch(checked = draft.openWeights == true, onCheckedChange = { draft = draft.copy(openWeights = if (it) true else null) },
                            modifier = Modifier.semantics { contentDescription = "Open weights only" })
                    }
                    SectionHeader("Knowledge cutoff")
                    FlowRow(Modifier.padding(horizontal = Dimens.lg), horizontalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                        listOf("2024", "2025", "2026").forEach { year ->
                            val cutoff = "$year-01"
                            FilterChip(selected = draft.knowledgeAfter == cutoff, shape = MaterialTheme.shapes.small,
                                onClick = { draft = draft.copy(knowledgeAfter = if (draft.knowledgeAfter == cutoff) null else cutoff) },
                                label = { Text("$year+") })
                        }
                    }
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = Dimens.lg, vertical = Dimens.sm)) {
                        SegmentedButton(selected = showLabs, onClick = { showLabs = true }, shape = SegmentedButtonDefaults.itemShape(0, 2, MaterialTheme.shapes.small)) {
                            Text("Labs (${labs.size})")
                        }
                        SegmentedButton(selected = !showLabs, onClick = { showLabs = false }, shape = SegmentedButtonDefaults.itemShape(1, 2, MaterialTheme.shapes.small)) {
                            Text("Providers (${providers.size})")
                        }
                    }
                    SearchField(if (showLabs) labQuery else providerQuery,
                        { if (showLabs) labQuery = it else providerQuery = it },
                        if (showLabs) "Search labs" else "Search providers",
                        Modifier.fillMaxWidth().padding(horizontal = Dimens.lg, vertical = Dimens.sm))
                }
                if (showLabs) {
                    items(visibleLabs, key = { "lab:${it.id}" }) { lab ->
                        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .toggleable(value = lab.id in draft.labIds, role = Role.Checkbox,
                                onValueChange = { draft = draft.copy(labIds = draft.labIds.toggle(lab.id)) })
                            .padding(horizontal = Dimens.lg), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                            Checkbox(checked = lab.id in draft.labIds, onCheckedChange = null)
                            LabLogo(lab.id, lab.name, 20.dp)
                            Text(lab.name, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                } else {
                    items(visibleProviders, key = { "provider:${it.id}" }) { provider ->
                        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .toggleable(value = provider.id in draft.providerIds, role = Role.Checkbox,
                                onValueChange = { draft = draft.copy(providerIds = draft.providerIds.toggle(provider.id)) })
                            .padding(horizontal = Dimens.lg), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                            Checkbox(checked = provider.id in draft.providerIds, onCheckedChange = null)
                            ProviderLogo(provider.id, provider.name, 20.dp)
                            Text(provider.name, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            CatalogDivider()
            Row(Modifier.fillMaxWidth().padding(Dimens.lg), horizontalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                OutlinedButton(onClick = { clearInput(); draft = ModelFilters(); providerQuery = ""; labQuery = "" }, modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LocalAppPalette.current.brandText),
                    border = BorderStroke(1.dp, LocalAppPalette.current.border)) { Text("Reset") }
                Button(onClick = { clearInput(); onApply(draft) }, modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.small) { Text("Apply") }
            }
        }
            }
        }
    }
}
