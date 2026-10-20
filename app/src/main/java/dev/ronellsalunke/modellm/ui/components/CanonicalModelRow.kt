package dev.ronellsalunke.modellm.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Schema
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.ronellsalunke.modellm.data.model.CanonicalModel
import dev.ronellsalunke.modellm.data.model.Capability
import dev.ronellsalunke.modellm.data.model.Modality
import dev.ronellsalunke.modellm.data.model.Model
import dev.ronellsalunke.modellm.ui.theme.Dimens
import dev.ronellsalunke.modellm.ui.theme.LocalAppPalette
import dev.ronellsalunke.modellm.ui.theme.MonoLabelStyle
import dev.ronellsalunke.modellm.ui.theme.MonoStyle
import dev.ronellsalunke.modellm.ui.util.capabilityLabel
import dev.ronellsalunke.modellm.ui.util.compactPricePair
import dev.ronellsalunke.modellm.ui.util.modalityLabel
import dev.ronellsalunke.modellm.ui.util.tokenLimit

fun CanonicalModel.capabilities(): List<Capability> = buildList {
    if (reasoning) add(Capability.Reasoning)
    if (toolCall) add(Capability.ToolCall)
    if (structuredOutput) add(Capability.StructuredOutput)
    if (attachment) add(Capability.Attachment)
    if (temperature) add(Capability.Temperature)
}

@Composable
fun ModelIcons(modalities: List<Modality>, capabilities: List<Capability>, openWeights: Boolean) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        modalities.forEach {
            Icon(
                when (it) {
                    Modality.Text -> Icons.Default.TextFields
                    Modality.Image -> Icons.Default.Image
                    Modality.Audio -> Icons.Default.GraphicEq
                    Modality.Video -> Icons.Default.Videocam
                    Modality.Pdf -> Icons.Default.Description
                },
                contentDescription = "Input: ${modalityLabel(it)}",
                modifier = Modifier.size(16.dp),
                tint = LocalAppPalette.current.textSecondary,
            )
        }
        capabilities.forEach {
            Icon(
                when (it) {
                    Capability.Reasoning -> Icons.Default.Psychology
                    Capability.ToolCall -> Icons.Default.Build
                    Capability.StructuredOutput -> Icons.Default.Schema
                    Capability.Attachment -> Icons.Default.AttachFile
                    Capability.Temperature -> Icons.Default.Thermostat
                },
                contentDescription = capabilityLabel(it),
                modifier = Modifier.size(16.dp),
                tint = LocalAppPalette.current.textSecondary,
            )
        }
        if (openWeights)
            Icon(
                Icons.Default.LockOpen,
                "Open weights",
                Modifier.size(16.dp),
                tint = LocalAppPalette.current.textSecondary,
            )
    }
}

@Composable
fun CanonicalModelRow(model: CanonicalModel, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.lg, vertical = Dimens.sm),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.sm),
        ) {
            LabLogo(model.labId, model.labName, 20.dp)
            Text(
                model.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            model.id,
            style = MonoLabelStyle,
            color = LocalAppPalette.current.textTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
    CatalogDivider()
}

@Composable
fun CanonicalProviderRow(model: Model, labName: String, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(Dimens.lg),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.sm),
        ) {
            ProviderLogo(model.providerId, model.providerName, 20.dp)
            Text(model.providerName, style = MaterialTheme.typography.titleSmall)
            Text(
                labName,
                style = MaterialTheme.typography.bodySmall,
                color = LocalAppPalette.current.textTertiary,
            )
        }
        Text(model.id, style = MonoStyle)
        Text(
            "${tokenLimit(model.limits.context)} ctx · " +
                "${tokenLimit(model.limits.output)} out · " +
                "${compactPricePair(model.cost.input, model.cost.output)} /1M",
            style = MonoLabelStyle,
        )
        ModelIcons(emptyList(), model.capabilities(), model.openWeights)
    }
    CatalogDivider()
}
