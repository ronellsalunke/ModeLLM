package dev.ronellsalunke.modellm.ui.licenses

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.util.author
import dev.ronellsalunke.modellm.R
import dev.ronellsalunke.modellm.ui.components.CatalogDivider
import dev.ronellsalunke.modellm.ui.components.CatalogRowMinHeight
import dev.ronellsalunke.modellm.ui.components.EmptyState
import dev.ronellsalunke.modellm.ui.components.rememberExternalLinkOpener
import dev.ronellsalunke.modellm.ui.components.SmallBackTopAppBar
import dev.ronellsalunke.modellm.ui.components.navigationAreaBottomPadding
import dev.ronellsalunke.modellm.ui.components.safeDrawingBottomPadding
import dev.ronellsalunke.modellm.ui.components.safeDrawingHorizontalPadding
import dev.ronellsalunke.modellm.ui.theme.Dimens
import dev.ronellsalunke.modellm.ui.theme.LocalAppPalette
import dev.ronellsalunke.modellm.ui.theme.MonoLabelStyle

@Composable
fun LicensesScreen(onBack: () -> Unit) {
    val libs by produceLibraries(R.raw.aboutlibraries)
    val libraries = libs?.libraries
    val openLink = rememberExternalLinkOpener()
    Column(
        Modifier.fillMaxSize()
            .background(LocalAppPalette.current.background)
            .safeDrawingHorizontalPadding()
    ) {
        SmallBackTopAppBar("Licenses", onBack)
        CatalogDivider()
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(
                bottom = Dimens.xl + safeDrawingBottomPadding() + navigationAreaBottomPadding(),
            ),
        ) {
            when {
                libraries == null -> item { EmptyState("Loading licenses…") }
                libraries.isEmpty() -> item { EmptyState("No libraries listed") }
                else ->
                    items(libraries, key = { it.uniqueId }) { library ->
                        LibraryRow(library, openLink)
                    }
            }
        }
    }
}

@Composable
private fun LibraryRow(library: Library, onOpenLink: (String) -> Unit) {
    val licenseUrl = library.licenses.firstNotNullOfOrNull { it.url }
    val onClick: (() -> Unit)? =
        licenseUrl?.let { url ->
            { onOpenLink(url) }
        }
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth()
                .heightIn(min = CatalogRowMinHeight)
                .then(
                    if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
                )
                .padding(horizontal = Dimens.lg, vertical = Dimens.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Dimens.sm),
            ) {
                Text(
                    library.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val author = library.author
                if (author.isNotBlank()) {
                    Text(
                        author,
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalAppPalette.current.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    librarySummary(library),
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

private fun librarySummary(library: Library): String =
    listOfNotNull(
            library.artifactVersion,
            library.licenses
                .joinToString { it.spdxId ?: it.name }
                .takeIf { it.isNotEmpty() },
        )
        .joinToString(" · ")
