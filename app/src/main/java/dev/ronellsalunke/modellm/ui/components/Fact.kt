package dev.ronellsalunke.modellm.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.ronellsalunke.modellm.data.model.Provider
import dev.ronellsalunke.modellm.ui.theme.Dimens
import dev.ronellsalunke.modellm.ui.theme.LocalAppPalette
import dev.ronellsalunke.modellm.ui.theme.MonoStyle
import dev.ronellsalunke.modellm.ui.util.groupedNumber

data class Fact(
    val label: String,
    val value: String?,
    val mono: Boolean = false,
    val fullWidth: Boolean = false,
    val newRow: Boolean = false,
)

@Composable
fun FactCell(label: String, value: String?, modifier: Modifier = Modifier, mono: Boolean = false) {
    Column(modifier.padding(Dimens.md), verticalArrangement = Arrangement.spacedBy(Dimens.xs)) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = LocalAppPalette.current.textTertiary,
        )
        Text(value ?: "–", style = if (mono) MonoStyle else MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun FactGrid(facts: List<Fact>, compactColumns: Int = 1) {
    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = Dimens.lg)) {
        val columns =
            when {
                maxWidth < 480.dp -> compactColumns.coerceAtLeast(1)
                maxWidth >= 600.dp -> 3
                else -> 2
            }
        val rows =
            buildList<List<Fact>> {
                var row = mutableListOf<Fact>()
                facts.forEach { fact ->
                    if (fact.fullWidth || fact.newRow) {
                        if (row.isNotEmpty()) {
                            add(row)
                            row = mutableListOf()
                        }
                    }
                    if (fact.fullWidth) {
                        add(listOf(fact))
                    } else {
                        row.add(fact)
                        if (row.size == columns) {
                            add(row)
                            row = mutableListOf()
                        }
                    }
                }
                if (row.isNotEmpty()) add(row)
            }
        Column(Modifier.border(1.dp, LocalAppPalette.current.border, MaterialTheme.shapes.small)) {
            rows.forEachIndexed { index, row ->
                if (index > 0) CatalogDivider()
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                    row.forEachIndexed { column, fact ->
                        if (column > 0) VerticalDivider(color = LocalAppPalette.current.border)
                        FactCell(fact.label, fact.value, Modifier.weight(1f), fact.mono)
                    }
                }
            }
        }
    }
}

@Composable
fun ProviderFacts(provider: Provider, showIdentity: Boolean = true) {
    if (showIdentity) {
        Row(
            Modifier.padding(horizontal = Dimens.lg, vertical = Dimens.sm),
            horizontalArrangement = Arrangement.spacedBy(Dimens.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProviderLogo(provider.id, provider.name)
            Text(provider.name, style = MaterialTheme.typography.titleMedium)
        }
    }
    FactGrid(
        listOf(
            Fact("Models", groupedNumber(provider.modelCount)),
            Fact("Provider ID", provider.id, mono = true),
            Fact("npm", provider.npm, mono = true, fullWidth = true),
            Fact("API", provider.api, mono = true, fullWidth = true),
        ),
        compactColumns = 2,
    )
    SectionHeader("Environment keys")
    Text(
        provider.env.takeIf { it.isNotEmpty() }?.joinToString("\n") ?: "–",
        style = MonoStyle,
        modifier = Modifier.padding(horizontal = Dimens.lg),
    )
    FlowRow(Modifier.padding(Dimens.lg), horizontalArrangement = Arrangement.spacedBy(Dimens.sm)) {
        provider.doc?.let { ExternalLink("Documentation", it) }
        provider.api?.let { ExternalLink("API", it) }
    }
}
