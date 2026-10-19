package dev.ronellsalunke.modellm.ui.components

import android.content.ActivityNotFoundException
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import dev.ronellsalunke.modellm.data.model.*
import dev.ronellsalunke.modellm.data.repository.SyncState
import dev.ronellsalunke.modellm.ui.theme.*
import dev.ronellsalunke.modellm.ui.util.*
import kotlinx.coroutines.delay

@Composable
fun ProviderLogo(providerId: String, name: String, size: Dp = 24.dp) {
    CatalogLogo("https://models.dev/logos/$providerId.svg", name, size)
}

@Composable
fun LabLogo(labId: String, name: String, size: Dp = 24.dp) {
    CatalogLogo("https://models.dev/logos/labs/$labId.svg", name, size)
}

@Composable
private fun CatalogLogo(url: String, name: String, size: Dp) {
    var loaded by remember(url) { mutableStateOf(false) }
    Box(
        Modifier.size(size)
            .semantics(mergeDescendants = true) { contentDescription = "$name logo" },
        contentAlignment = Alignment.Center,
    ) {
        if (!loaded) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(name.take(2).uppercase(), style = MonoLabelStyle, color = MaterialTheme.colorScheme.onSurface)
        }
        AsyncImage(
            model = url,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
            onLoading = { loaded = false },
            onError = { loaded = false },
            onSuccess = { loaded = true },
        )
    }
}

@Composable
fun CatalogDivider() = HorizontalDivider(thickness = Dimens.hairline, color = LocalAppPalette.current.border)

@Composable
fun SearchField(query: String, onQueryChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    val palette = LocalAppPalette.current
    val focusManager = LocalFocusManager.current
    Row(
        modifier.heightIn(min = 48.dp).border(1.dp, palette.border, MaterialTheme.shapes.small)
            .background(palette.surface, MaterialTheme.shapes.small).padding(start = Dimens.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = palette.textTertiary, modifier = Modifier.size(20.dp))
        BasicTextField(
            value = query, onValueChange = onQueryChange, singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(palette.brand),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            modifier = Modifier.weight(1f).heightIn(min = 48.dp).padding(horizontal = Dimens.sm).semantics { contentDescription = label },
            decorationBox = { field ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) Text(label, color = palette.textTertiary, style = MaterialTheme.typography.bodyMedium)
                    field()
                }
            },
        )
        if (query.isNotEmpty()) IconButton(onClick = {
            onQueryChange("")
            focusManager.clearFocus()
        }, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Clear search", modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(title.uppercase(), style = MaterialTheme.typography.labelSmall,
        color = LocalAppPalette.current.textTertiary,
        modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.lg, vertical = Dimens.md))
}

@Composable
private fun LabelTile(label: String, modifier: Modifier = Modifier) {
    Text(label.uppercase(), style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
        color = LocalAppPalette.current.textSecondary,
        modifier = modifier.border(1.dp, LocalAppPalette.current.border, MaterialTheme.shapes.small)
            .background(LocalAppPalette.current.surface, MaterialTheme.shapes.small)
            .padding(horizontal = 6.dp, vertical = 3.dp))
}

@Composable
fun CapabilityBadge(capability: Capability) = LabelTile(capabilityLabel(capability))

@Composable
fun StatusChip(status: String) {
    val deprecated = status.equals("deprecated", ignoreCase = true)
    val palette = LocalAppPalette.current
    val color = if (deprecated) MaterialTheme.colorScheme.onErrorContainer else palette.brandText
    val background = if (deprecated) MaterialTheme.colorScheme.errorContainer else palette.brand.copy(alpha = 0.12f)
    Text(status.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
        color = color, modifier = Modifier.background(background, RoundedCornerShape(4.dp))
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 1.dp))
}

@Composable
fun ModalityTile(modality: Modality) = LabelTile(modalityLabel(modality))

@Composable
fun PriceText(input: Double?, output: Double?) {
    Text(pricePair(input, output), style = MonoStyle, color = LocalAppPalette.current.textSecondary)
}

fun Model.capabilities(): List<Capability> = buildList {
    if (reasoning) add(Capability.Reasoning)
    if (toolCall) add(Capability.ToolCall)
    if (structuredOutput) add(Capability.StructuredOutput)
    if (attachment) add(Capability.Attachment)
    if (temperature) add(Capability.Temperature)
}

@Composable
fun ModelRow(model: Model, onClick: () -> Unit) {
    val capabilities = remember(model) { model.capabilities() }
    Column(
        Modifier.fillMaxWidth().heightIn(min = 96.dp).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.sm), verticalAlignment = Alignment.CenterVertically) {
            ProviderLogo(model.providerId, model.providerName, 20.dp)
            Text(model.name, style = MaterialTheme.typography.titleSmall,
                 color = if (model.status.equals("deprecated", true)) LocalAppPalette.current.textSecondary else MaterialTheme.colorScheme.onSurface,
                 modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            model.status?.let { StatusChip(it) }
            if (model.experimental && !model.status.equals("experimental", true)) StatusChip("Experimental")
        }
        Text("${model.providerName} · ${model.family ?: model.id}",
            style = MaterialTheme.typography.bodySmall, color = LocalAppPalette.current.textTertiary,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("${tokenLimit(model.limits.context)} context · ${compactPricePair(model.cost.input, model.cost.output)} /1M",
            style = MonoLabelStyle, color = LocalAppPalette.current.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (capabilities.isNotEmpty() || model.openWeights) Row(
            horizontalArrangement = Arrangement.spacedBy(Dimens.xs),
        ) {
            capabilities.forEach {
                CapabilityIcon(when (it) {
                    Capability.Reasoning -> Icons.Default.Psychology
                    Capability.ToolCall -> Icons.Default.Build
                    Capability.StructuredOutput -> Icons.Default.Schema
                    Capability.Attachment -> Icons.Default.AttachFile
                    Capability.Temperature -> Icons.Default.Thermostat
                }, capabilityLabel(it))
            }
            if (model.openWeights) CapabilityIcon(Icons.Default.LockOpen, "Open weights")
        }
    }
    CatalogDivider()
}

@Composable
private fun CapabilityIcon(icon: ImageVector, label: String) {
    Box(Modifier.size(24.dp).border(1.dp, LocalAppPalette.current.border, RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(14.dp), tint = LocalAppPalette.current.textSecondary)
    }
}

data class Fact(val label: String, val value: String?, val mono: Boolean = false, val fullWidth: Boolean = false)

@Composable
fun FactCell(label: String, value: String?, modifier: Modifier = Modifier, mono: Boolean = false) {
    Column(modifier.padding(Dimens.md), verticalArrangement = Arrangement.spacedBy(Dimens.xs)) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = LocalAppPalette.current.textTertiary)
        Text(value ?: "–", style = if (mono) MonoStyle else MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun FactGrid(facts: List<Fact>) {
    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = Dimens.lg)) {
        val columns = when { maxWidth < 480.dp -> 1; maxWidth >= 600.dp -> 3; else -> 2 }
        val rows = buildList<List<Fact>> {
            var row = mutableListOf<Fact>()
            facts.forEach { fact ->
                if (fact.fullWidth) {
                    if (row.isNotEmpty()) { add(row); row = mutableListOf() }
                    add(listOf(fact))
                } else {
                    row.add(fact)
                    if (row.size == columns) { add(row); row = mutableListOf() }
                }
            }
            if (row.isNotEmpty()) add(row)
        }
        Column(Modifier.border(1.dp, LocalAppPalette.current.border, MaterialTheme.shapes.small)) {
            rows.forEachIndexed { index, row ->
                if (index > 0) CatalogDivider()
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                    val count = if (row.first().fullWidth) 1 else columns
                    repeat(count) { column ->
                        if (column > 0) VerticalDivider(color = LocalAppPalette.current.border)
                        val fact = row.getOrNull(column)
                        if (fact != null) FactCell(fact.label, fact.value, Modifier.weight(1f), fact.mono)
                        else Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun rememberRelativeTime(timestamp: Long?): String {
    val now by produceState(System.currentTimeMillis(), timestamp) {
        while (true) { value = System.currentTimeMillis(); delay(60_000) }
    }
    return timestamp?.let { relativeTime(it, now) } ?: "Never"
}

@Composable
fun SyncBanner(lastSyncedAt: Long?, state: SyncState, onRetry: () -> Unit) {
    val time = rememberRelativeTime(lastSyncedAt)
    if (lastSyncedAt != null || state is SyncState.Failed || state is SyncState.Running) {
        Column(Modifier.fillMaxWidth().background(LocalAppPalette.current.surface).padding(horizontal = Dimens.lg)) {
            if (lastSyncedAt != null) Text("Updated $time", style = MaterialTheme.typography.bodySmall,
                color = LocalAppPalette.current.textSecondary, modifier = Modifier.padding(vertical = Dimens.sm))
            if (state is SyncState.Running) Text("Refreshing catalog…", style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = Dimens.sm))
            if (state is SyncState.Failed) {
                Text(if (lastSyncedAt != null) "Couldn't refresh. Showing saved catalog." else "Couldn't download the catalog.",
                    style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = onRetry,
                    colors = ButtonDefaults.textButtonColors(contentColor = LocalAppPalette.current.brandText)) { Text("Retry") }
            }
        }
        CatalogDivider()
    }
}

@Composable
fun EmptyState(title: String, message: String? = null, actionLabel: String? = null, onAction: () -> Unit = {}) {
    Column(Modifier.fillMaxWidth().padding(Dimens.xl), verticalArrangement = Arrangement.spacedBy(Dimens.md)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        message?.let { Text(it, color = LocalAppPalette.current.textSecondary, style = MaterialTheme.typography.bodyMedium) }
        actionLabel?.let { OutlinedButton(onClick = onAction, shape = MaterialTheme.shapes.small,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = LocalAppPalette.current.brandText),
            border = BorderStroke(1.dp, LocalAppPalette.current.border)) { Text(it) } }
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit) =
    EmptyState("Couldn't load the catalog", message, "Retry", onRetry)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val state = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier,
        state = state,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = state,
                isRefreshing = isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter),
                color = LocalAppPalette.current.brand,
            )
        },
        content = content,
    )
}

@Composable
fun ExternalLink(label: String, url: String, modifier: Modifier = Modifier) {
    val handler = LocalUriHandler.current
    val context = LocalContext.current
    TextButton(onClick = {
        try {
            val scheme = url.toUri().scheme
            if (scheme != "https" && scheme != "http") throw IllegalArgumentException()
            handler.openUri(url)
        } catch (_: IllegalArgumentException) {
            Toast.makeText(context, "Couldn't open this link", Toast.LENGTH_SHORT).show()
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "No app can open this link", Toast.LENGTH_SHORT).show()
        }
    }, modifier = modifier.heightIn(min = 48.dp), shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.textButtonColors(contentColor = LocalAppPalette.current.brandText),
        contentPadding = PaddingValues(horizontal = 4.dp)) {
        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, textDecoration = TextDecoration.Underline)
    }
}

@Composable
fun ProviderFacts(provider: Provider, showIdentity: Boolean = true) {
    if (showIdentity) Row(Modifier.padding(horizontal = Dimens.lg, vertical = Dimens.sm),
        horizontalArrangement = Arrangement.spacedBy(Dimens.sm), verticalAlignment = Alignment.CenterVertically) {
        ProviderLogo(provider.id, provider.name)
        Text(provider.name, style = MaterialTheme.typography.titleMedium)
    }
    FactGrid(listOf(Fact("Models", groupedNumber(provider.modelCount)),
        Fact("Provider ID", provider.id, mono = true),
        Fact("npm", provider.npm, mono = true, fullWidth = true), Fact("API", provider.api, mono = true, fullWidth = true)))
    SectionHeader("Environment keys")
    Text(provider.env.takeIf { it.isNotEmpty() }?.joinToString("\n") ?: "–", style = MonoStyle,
        modifier = Modifier.padding(horizontal = Dimens.lg))
    FlowRow(Modifier.padding(Dimens.lg), horizontalArrangement = Arrangement.spacedBy(Dimens.sm)) {
        provider.doc?.let { ExternalLink("Documentation", it) }
        provider.api?.let { ExternalLink("API", it) }
    }
}
