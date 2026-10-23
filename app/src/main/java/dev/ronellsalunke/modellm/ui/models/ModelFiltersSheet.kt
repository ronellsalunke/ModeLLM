package dev.ronellsalunke.modellm.ui.models

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.ronellsalunke.modellm.data.model.Capability
import dev.ronellsalunke.modellm.data.model.Lab
import dev.ronellsalunke.modellm.data.model.Modality
import dev.ronellsalunke.modellm.data.model.ModelFilters
import dev.ronellsalunke.modellm.data.model.Provider
import dev.ronellsalunke.modellm.ui.components.CatalogDivider
import dev.ronellsalunke.modellm.ui.components.LabLogo
import dev.ronellsalunke.modellm.ui.components.LocalCatalogShortcuts
import dev.ronellsalunke.modellm.ui.components.LocalWideLayout
import dev.ronellsalunke.modellm.ui.components.ModalSideSheet
import dev.ronellsalunke.modellm.ui.components.ProviderLogo
import dev.ronellsalunke.modellm.ui.components.SearchField
import dev.ronellsalunke.modellm.ui.components.SectionHeader
import dev.ronellsalunke.modellm.ui.components.SingleChoiceButtonGroup
import dev.ronellsalunke.modellm.ui.theme.Dimens
import dev.ronellsalunke.modellm.ui.theme.LocalAppPalette
import dev.ronellsalunke.modellm.ui.util.capabilityLabel
import dev.ronellsalunke.modellm.ui.util.modalityLabel
import kotlinx.coroutines.launch

private const val SheetMaxHeightFraction = 0.9f
private val SheetCornerRadius = 16.dp
private val ItemMinHeight = 48.dp
private val LogoSize = 20.dp
private val BorderStrokeWidth = 1.dp
private val CutoffYears = listOf("2024", "2025", "2026")

private fun <T> Set<T>.toggle(value: T): Set<T> = if (value in this) this - value else this + value

/**
 * Filter sheet. Bottom sheet on compact windows; side sheet on medium and larger windows, where
 * a bottom sheet would leave the catalog hidden behind a stretched surface.
 */
@Composable
fun ModelFiltersSheet(
    filters: ModelFilters,
    providers: List<Provider>,
    labs: List<Lab>,
    onDismiss: () -> Unit,
    onUpdateFilters: ((ModelFilters) -> ModelFilters) -> Unit,
) {
    var providerQuery by rememberSaveable { mutableStateOf("") }
    var labQuery by rememberSaveable { mutableStateOf("") }
    var showLabs by rememberSaveable { mutableStateOf(true) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    DisposableEffect(focusManager, keyboardController) {
        onDispose {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }
    // Shortcuts must not act on the catalog behind the sheet.
    CompositionLocalProvider(LocalCatalogShortcuts provides null) {
        if (LocalWideLayout.current) {
            ModalSideSheet(onDismiss = onDismiss) {
                FilterSheetContent(
                    filters = filters,
                    providers = providers,
                    labs = labs,
                    onHide = onDismiss,
                    onUpdateFilters = onUpdateFilters,
                    providerQuery = providerQuery,
                    onProviderQueryChange = { providerQuery = it },
                    labQuery = labQuery,
                    onLabQueryChange = { labQuery = it },
                    showLabs = showLabs,
                    onShowLabsChange = { showLabs = it },
                    modifier = Modifier.fillMaxSize().imePadding(),
                )
            }
        } else {
            FilterBottomSheet(onDismiss = onDismiss) { onHide ->
                FilterSheetContent(
                    filters = filters,
                    providers = providers,
                    labs = labs,
                    onHide = onHide,
                    onUpdateFilters = onUpdateFilters,
                    providerQuery = providerQuery,
                    onProviderQueryChange = { providerQuery = it },
                    labQuery = labQuery,
                    onLabQueryChange = { labQuery = it },
                    showLabs = showLabs,
                    onShowLabsChange = { showLabs = it },
                    modifier =
                        Modifier.fillMaxWidth()
                            .fillMaxHeight(SheetMaxHeightFraction)
                            .imePadding(),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterBottomSheet(
    onDismiss: () -> Unit,
    content: @Composable (onHide: () -> Unit) -> Unit,
) {
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
        shape = RoundedCornerShape(topStart = SheetCornerRadius, topEnd = SheetCornerRadius),
    ) {
        content(hideSheet)
    }
}

@Composable
private fun FilterSheetContent(
    filters: ModelFilters,
    providers: List<Provider>,
    labs: List<Lab>,
    onHide: () -> Unit,
    onUpdateFilters: ((ModelFilters) -> ModelFilters) -> Unit,
    providerQuery: String,
    onProviderQueryChange: (String) -> Unit,
    labQuery: String,
    onLabQueryChange: (String) -> Unit,
    showLabs: Boolean,
    onShowLabsChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val visibleLabs = remember(labs, labQuery) {
        labs.filter { it.name.contains(labQuery, true) || it.id.contains(labQuery, true) }
    }
    val visibleProviders = remember(providers, providerQuery) {
        providers.filter {
            it.name.contains(providerQuery, true) || it.id.contains(providerQuery, true)
        }
    }
    Column(modifier) {
        Text(
            "Filters",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = Dimens.lg, vertical = Dimens.sm),
        )
        LazyColumn(Modifier.weight(1f)) {
            item {
                ModalitiesAndCapabilitiesSection(filters, onUpdateFilters)
                KnowledgeCutoffSection(filters.knowledgeAfter) { cutoff ->
                    onUpdateFilters {
                        it.copy(knowledgeAfter = if (it.knowledgeAfter == cutoff) null else cutoff)
                    }
                }
                FilterTargetSection(
                    showLabs = showLabs,
                    labsCount = labs.size,
                    providersCount = providers.size,
                    query = if (showLabs) labQuery else providerQuery,
                    onToggleLabs = onShowLabsChange,
                    onQueryChange = if (showLabs) onLabQueryChange else onProviderQueryChange,
                )
            }
            filterEntityItems(showLabs, visibleLabs, visibleProviders, filters, onUpdateFilters)
        }
        CatalogDivider()
        FilterActionButtons(
            onReset = {
                onUpdateFilters { ModelFilters() }
                onProviderQueryChange("")
                onLabQueryChange("")
            },
            onDone = onHide,
        )
    }
}

private fun LazyListScope.filterEntityItems(
    showLabs: Boolean,
    visibleLabs: List<Lab>,
    visibleProviders: List<Provider>,
    filters: ModelFilters,
    onUpdateFilters: ((ModelFilters) -> ModelFilters) -> Unit,
) {
    if (showLabs) {
        items(visibleLabs, key = { "lab:${it.id}" }) { lab ->
            LabFilterRow(lab, lab.id in filters.labIds) {
                onUpdateFilters { it.copy(labIds = it.labIds.toggle(lab.id)) }
            }
        }
    } else {
        items(visibleProviders, key = { "provider:${it.id}" }) { provider ->
            ProviderFilterRow(provider, provider.id in filters.providerIds) {
                onUpdateFilters {
                    it.copy(providerIds = it.providerIds.toggle(provider.id))
                }
            }
        }
    }
}

@Composable
private fun ModalitiesAndCapabilitiesSection(
    filters: ModelFilters,
    onUpdateFilters: ((ModelFilters) -> ModelFilters) -> Unit,
) {
    SectionHeader("Input modalities · match all")
    FlowRow(
        Modifier.padding(horizontal = Dimens.lg),
        horizontalArrangement = Arrangement.spacedBy(Dimens.sm),
    ) {
        Modality.entries.forEach { modality ->
            FilterChip(
                selected = modality in filters.inputModalities,
                shape = MaterialTheme.shapes.small,
                onClick = {
                    onUpdateFilters {
                        it.copy(inputModalities = it.inputModalities.toggle(modality))
                    }
                },
                label = { Text(modalityLabel(modality)) },
            )
        }
    }
    SectionHeader("Capabilities · match all")
    FlowRow(
        Modifier.padding(horizontal = Dimens.lg),
        horizontalArrangement = Arrangement.spacedBy(Dimens.sm),
    ) {
        Capability.entries.forEach { capability ->
            FilterChip(
                selected = capability in filters.capabilities,
                shape = MaterialTheme.shapes.small,
                onClick = {
                    onUpdateFilters {
                        it.copy(capabilities = it.capabilities.toggle(capability))
                    }
                },
                label = { Text(capabilityLabel(capability)) },
            )
        }
    }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Dimens.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Open weights only", modifier = Modifier.weight(1f))
        Switch(
            checked = filters.openWeights == true,
            onCheckedChange = { checked ->
                onUpdateFilters { it.copy(openWeights = if (checked) true else null) }
            },
            modifier = Modifier.semantics { contentDescription = "Open weights only" },
        )
    }
}

@Composable
private fun KnowledgeCutoffSection(
    selectedCutoff: String?,
    onSelectCutoff: (String) -> Unit,
) {
    SectionHeader("Knowledge cutoff")
    FlowRow(
        Modifier.padding(horizontal = Dimens.lg),
        horizontalArrangement = Arrangement.spacedBy(Dimens.sm),
    ) {
        CutoffYears.forEach { year ->
            val cutoff = "$year-01"
            FilterChip(
                selected = selectedCutoff == cutoff,
                shape = MaterialTheme.shapes.small,
                onClick = { onSelectCutoff(cutoff) },
                label = { Text("$year+") },
            )
        }
    }
}

@Composable
private fun FilterTargetSection(
    showLabs: Boolean,
    labsCount: Int,
    providersCount: Int,
    query: String,
    onToggleLabs: (Boolean) -> Unit,
    onQueryChange: (String) -> Unit,
) {
    SingleChoiceButtonGroup(
        labels = listOf("Labs ($labsCount)", "Providers ($providersCount)"),
        selectedIndex = if (showLabs) 0 else 1,
        onSelect = { onToggleLabs(it == 0) },
        modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.lg, vertical = Dimens.sm),
    )
    SearchField(
        query = query,
        onQueryChange = onQueryChange,
        label = if (showLabs) "Search labs" else "Search providers",
        modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.lg, vertical = Dimens.sm),
    )
}

@Composable
private fun LabFilterRow(
    lab: Lab,
    selected: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth()
            .heightIn(min = ItemMinHeight)
            .toggleable(value = selected, role = Role.Checkbox, onValueChange = { onToggle() })
            .padding(horizontal = Dimens.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.sm),
    ) {
        Checkbox(checked = selected, onCheckedChange = null)
        LabLogo(lab.id, lab.name, LogoSize)
        Text(lab.name, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ProviderFilterRow(
    provider: Provider,
    selected: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth()
            .heightIn(min = ItemMinHeight)
            .toggleable(value = selected, role = Role.Checkbox, onValueChange = { onToggle() })
            .padding(horizontal = Dimens.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.sm),
    ) {
        Checkbox(checked = selected, onCheckedChange = null)
        ProviderLogo(provider.id, provider.name, LogoSize)
        Text(provider.name, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun FilterActionButtons(
    onReset: () -> Unit,
    onDone: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(Dimens.lg),
        horizontalArrangement = Arrangement.spacedBy(Dimens.sm),
    ) {
        OutlinedButton(
            onClick = onReset,
            modifier = Modifier.weight(1f),
            shape = MaterialTheme.shapes.small,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = LocalAppPalette.current.brandText,
            ),
            border = BorderStroke(BorderStrokeWidth, LocalAppPalette.current.border),
        ) {
            Text("Reset")
        }
        Button(
            onClick = onDone,
            modifier = Modifier.weight(1f),
            shape = MaterialTheme.shapes.small,
        ) {
            Text("Done")
        }
    }
}
