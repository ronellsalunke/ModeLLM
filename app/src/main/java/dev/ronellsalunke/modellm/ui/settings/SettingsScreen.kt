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
    val syncIntervalMillis = 15 * 60 * 1000L
    val now = System.currentTimeMillis()
    val lastSyncedAt = state.lastSyncedAt
    val canSync = lastSyncedAt == null || now - lastSyncedAt >= syncIntervalMillis
    val remainingMinutes = lastSyncedAt
        ?.let { last -> ((syncIntervalMillis - (now - last)).coerceAtLeast(0) / 60_000L + 1L).coerceAtLeast(1L) }
    Column(Modifier.fillMaxSize().background(LocalAppPalette.current.background)) {
        BackHeader("Settings", onBack)
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = Dimens.xl)) {
            item {
                SectionHeader("Appearance")
                Column(Modifier.padding(horizontal = Dimens.lg), verticalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                    Text("Theme", style = MaterialTheme.typography.bodyMedium)
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        ThemeMode.entries.forEachIndexed { index, mode ->
                            SegmentedButton(selected = state.settings.themeMode == mode, onClick = { viewModel.setThemeMode(mode) },
                                shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size, MaterialTheme.shapes.small)) {
                                Text(mode.name)
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Use wallpaper colors", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = state.settings.dynamicColor, onCheckedChange = viewModel::setDynamicColor, enabled = wallpaperSupported,
                            modifier = Modifier.semantics { contentDescription = "Use wallpaper colors" })
                    }
                    Text("Material You uses your wallpaper colors.", style = MaterialTheme.typography.bodySmall, color = LocalAppPalette.current.textSecondary)
                    if (!wallpaperSupported) Text("Requires Android 12 or later.", style = MaterialTheme.typography.bodySmall, color = LocalAppPalette.current.textSecondary)
                    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                }
                Spacer(Modifier.height(Dimens.lg))
                CatalogDivider()
                SectionHeader("Catalog")
                Column(Modifier.padding(horizontal = Dimens.lg), verticalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                    Text("Last updated: $updated", style = MaterialTheme.typography.bodyMedium)
                    Text(when (val sync = state.syncState) {
                        SyncState.Idle -> when {
                            state.lastSyncedAt == null -> "Ready to sync"
                            canSync -> "Catalog can be refreshed"
                            else -> "Catalog is up to date"
                        }
                        SyncState.Running -> "Refreshing catalog…"
                        is SyncState.Success -> "Catalog is up to date"
                        is SyncState.Failed -> "Couldn't refresh: ${sync.message}"
                    }, style = MaterialTheme.typography.bodySmall, color = LocalAppPalette.current.textSecondary)
                    Button(onClick = viewModel::refresh,
                        enabled = canSync && state.syncState !is SyncState.Running,
                        shape = MaterialTheme.shapes.small) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Sync now")
                    }
                    Text(
                        if (!canSync && state.syncState !is SyncState.Running) {
                            "Next refresh available in $remainingMinutes min."
                        } else {
                            "Refreshes have a 15-minute minimum interval. The catalog also refreshes daily when you're connected."
                        },
                        style = MaterialTheme.typography.bodySmall, color = LocalAppPalette.current.textSecondary,
                    )
                }
                Spacer(Modifier.height(Dimens.lg))
                CatalogDivider()
                SectionHeader("About")
                Column(Modifier.padding(horizontal = Dimens.lg), verticalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                    Text("ModeLLM $version", style = MonoStyle)
                    Text("Model data and provider logos come from models.dev.", style = MaterialTheme.typography.bodyMedium)
                    ExternalLink("Data from models.dev", "https://models.dev")
                    ExternalLink("models.dev on GitHub", "https://github.com/anomalyco/models.dev")
                    Text("Rubik and IBM Plex Mono are licensed under the SIL Open Font License (OFL).", style = MaterialTheme.typography.bodySmall,
                        color = LocalAppPalette.current.textSecondary)
                    ExternalLink("Rubik font license", "https://github.com/google/fonts/blob/main/ofl/rubik/OFL.txt")
                    ExternalLink("IBM Plex Mono font license", "https://github.com/google/fonts/blob/main/ofl/ibmplexmono/OFL.txt")
                }
            }
        }
    }
}
