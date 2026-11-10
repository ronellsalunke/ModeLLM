package dev.ronellsalunke.modellm.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import dev.ronellsalunke.modellm.data.model.CanonicalModel
import dev.ronellsalunke.modellm.data.model.Capability
import dev.ronellsalunke.modellm.ui.theme.Dimens
import dev.ronellsalunke.modellm.ui.theme.LocalAppPalette
import dev.ronellsalunke.modellm.ui.theme.MonoLabelStyle

fun CanonicalModel.capabilities(): List<Capability> = buildList {
    if (reasoning) add(Capability.Reasoning)
    if (toolCall) add(Capability.ToolCall)
    if (structuredOutput) add(Capability.StructuredOutput)
    if (attachment) add(Capability.Attachment)
    if (temperature) add(Capability.Temperature)
}

@Composable
fun CanonicalModelRow(model: CanonicalModel, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth()
                .heightIn(min = CatalogRowMinHeight)
                .clickable(onClick = onClick)
                .padding(horizontal = Dimens.lg, vertical = Dimens.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.md),
        ) {
            LabLogo(model.labId, model.labName)
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Dimens.xs),
            ) {
                Text(
                    model.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    model.id,
                    style = MonoLabelStyle,
                    color = LocalAppPalette.current.textTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        CatalogDivider()
    }
}
