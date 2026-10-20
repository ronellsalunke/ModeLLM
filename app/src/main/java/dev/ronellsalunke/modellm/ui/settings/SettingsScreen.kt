package dev.ronellsalunke.modellm.ui.settings

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ronellsalunke.modellm.data.repository.SyncState
import dev.ronellsalunke.modellm.data.settings.ThemeMode
import dev.ronellsalunke.modellm.ui.components.*
import dev.ronellsalunke.modellm.ui.theme.*
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val version = remember(context) { context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "–" }
    val updated = rememberRelativeTime(state.lastSyncedAt)
    val wallpaperSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    Column(Modifier.fillMaxSize().background(LocalAppPalette.current.background)) {
        SmallBackTopAppBar("Settings", onBack)
        CatalogDivider()
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = Dimens.xl)) {
            item {
                SectionHeader("Appearance")
                Column(Modifier.padding(horizontal = Dimens.lg), verticalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                    Text("Theme", style = MaterialTheme.typography.bodyMedium)
                    SingleChoiceButtonGroup(
                        labels = ThemeMode.entries.map { it.name },
                        selectedIndex = ThemeMode.entries.indexOf(state.settings.themeMode),
                        onSelect = { viewModel.setThemeMode(ThemeMode.entries[it]) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Use wallpaper colors", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = state.settings.dynamicColor, onCheckedChange = viewModel::setDynamicColor, enabled = wallpaperSupported,
                            modifier = Modifier.semantics { contentDescription = "Use wallpaper colors" })
                    }
                    if (!wallpaperSupported) Text("Requires Android 12 or later.", style = MaterialTheme.typography.bodySmall, color = LocalAppPalette.current.textSecondary)
                    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                }
                Spacer(Modifier.height(Dimens.lg))
                CatalogDivider()
                SectionHeader("Catalog")
                Column(Modifier.padding(horizontal = Dimens.lg), verticalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Last updated: $updated", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Button(onClick = viewModel::refresh,
                            enabled = state.syncState !is SyncState.Running,
                            shape = MaterialTheme.shapes.small) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Sync now")
                        }
                    }
                    Text(when (val sync = state.syncState) {
                        SyncState.Idle -> if (state.lastSyncedAt == null) "Ready to sync" else "Catalog can be refreshed"
                        SyncState.Running -> "Refreshing catalog…"
                        is SyncState.Success -> "Catalog is up to date"
                        is SyncState.Failed -> "Couldn't refresh: ${sync.message}"
                    }, style = MaterialTheme.typography.bodySmall, color = LocalAppPalette.current.textSecondary)
                    Text(
                        "The catalog refreshes daily when you're connected. Tap Sync now or pull down to refresh at any time.",
                        style = MaterialTheme.typography.bodySmall, color = LocalAppPalette.current.textSecondary,
                    )
                }
                Spacer(Modifier.height(Dimens.lg))
                CatalogDivider()
                SectionHeader("About")
                Column(Modifier.padding(horizontal = Dimens.lg), verticalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                    Text("ModeLLM $version", style = MonoStyle)
                    val linkColor = LocalAppPalette.current.brandText
                    Text(buildAnnotatedString {
                        append("Model data and provider logos come from ")
                        withLink(LinkAnnotation.Url("https://models.dev", TextLinkStyles(
                            style = SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
                        ))) {
                            append("models.dev")
                        }
                        append(".")
                    }, style = MaterialTheme.typography.bodyMedium)
                    Text(buildAnnotatedString {
                        withLink(LinkAnnotation.Url("https://github.com/google/fonts/blob/main/ofl/rubik/OFL.txt", TextLinkStyles(
                            style = SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
                        ))) {
                            append("Rubik")
                        }
                        append(" and ")
                        withLink(LinkAnnotation.Url("https://github.com/google/fonts/blob/main/ofl/ibmplexmono/OFL.txt", TextLinkStyles(
                            style = SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
                        ))) {
                            append("IBM Plex Mono")
                        }
                        append(" are licensed under the SIL Open Font License (OFL).")
                    }, style = MaterialTheme.typography.bodySmall, color = LocalAppPalette.current.textSecondary)
                }
            }
        }
    }
}
