package dev.ronellsalunke.modellm.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Schema
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.ronellsalunke.modellm.data.model.Capability
import dev.ronellsalunke.modellm.data.model.Model
import dev.ronellsalunke.modellm.ui.theme.Dimens
import dev.ronellsalunke.modellm.ui.theme.LocalAppPalette
import dev.ronellsalunke.modellm.ui.theme.MonoLabelStyle
import dev.ronellsalunke.modellm.ui.util.capabilityLabel
import dev.ronellsalunke.modellm.ui.util.compactPricePair
import dev.ronellsalunke.modellm.ui.util.tokenLimit

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
    val contextInfo = tokenLimit(model.limits.context)
    val priceInfo = compactPricePair(model.cost.input, model.cost.output)
    Column(
        Modifier.fillMaxWidth()
            .heightIn(min = 96.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
    ) {
        ModelHeaderRow(model)
        Text(
            "${model.providerName} · ${model.family ?: model.id}",
            style = MaterialTheme.typography.bodySmall,
            color = LocalAppPalette.current.textTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            "$contextInfo context · $priceInfo /1M",
            style = MonoLabelStyle,
            color = LocalAppPalette.current.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        ModelCapabilitiesRow(capabilities = capabilities, openWeights = model.openWeights)
    }
    CatalogDivider()
}

@Composable
private fun ModelHeaderRow(model: Model) {
    val isDeprecated = model.status.equals("deprecated", true)
    val nameColor =
        if (isDeprecated) LocalAppPalette.current.textSecondary
        else MaterialTheme.colorScheme.onSurface
    Row(
        horizontalArrangement = Arrangement.spacedBy(Dimens.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProviderLogo(model.providerId, model.providerName, 20.dp)
        Text(
            model.name,
            style = MaterialTheme.typography.titleSmall,
            color = nameColor,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        model.status?.let { StatusChip(it) }
        if (model.experimental && !model.status.equals("experimental", true)) {
            StatusChip("Experimental")
        }
    }
}

@Composable
private fun ModelCapabilitiesRow(capabilities: List<Capability>, openWeights: Boolean) {
    if (capabilities.isNotEmpty() || openWeights) {
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.xs)) {
            capabilities.forEach { capability ->
                CapabilityIcon(capabilityIcon(capability), capabilityLabel(capability))
            }
            if (openWeights) {
                CapabilityIcon(Icons.Default.LockOpen, "Open weights")
            }
        }
    }
}

private fun capabilityIcon(capability: Capability): ImageVector =
    when (capability) {
        Capability.Reasoning -> Icons.Default.Psychology
        Capability.ToolCall -> Icons.Default.Build
        Capability.StructuredOutput -> Icons.Default.Schema
        Capability.Attachment -> Icons.Default.AttachFile
        Capability.Temperature -> Icons.Default.Thermostat
    }

@Composable
private fun CapabilityIcon(icon: ImageVector, label: String) {
    Box(
        Modifier.size(24.dp).border(1.dp, LocalAppPalette.current.border, RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = label,
            modifier = Modifier.size(14.dp),
            tint = LocalAppPalette.current.textSecondary,
        )
    }
}
