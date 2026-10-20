package dev.ronellsalunke.modellm.ui.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ronellsalunke.modellm.data.repository.SyncState
import dev.ronellsalunke.modellm.data.settings.ThemeMode
import dev.ronellsalunke.modellm.ui.components.CatalogDivider
import dev.ronellsalunke.modellm.ui.components.rememberRelativeTime
import dev.ronellsalunke.modellm.ui.components.SectionHeader
import dev.ronellsalunke.modellm.ui.components.SingleChoiceButtonGroup
import dev.ronellsalunke.modellm.ui.components.SmallBackTopAppBar
import dev.ronellsalunke.modellm.ui.theme.Dimens
import dev.ronellsalunke.modellm.ui.theme.LocalAppPalette
import dev.ronellsalunke.modellm.ui.theme.MonoStyle
import org.koin.androidx.compose.koinViewModel

private const val ModelsDevUrl = "https://models.dev"
private const val RubikLicenseUrl =
    "https://github.com/google/fonts/blob/main/ofl/rubik/OFL.txt"
private const val IbmPlexMonoLicenseUrl =
    "https://github.com/google/fonts/blob/main/ofl/ibmplexmono/OFL.txt"

@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val version =
        remember(context) {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "–"
        }
    val updated = rememberRelativeTime(state.lastSyncedAt)
    val wallpaperSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    Column(Modifier.fillMaxSize().background(LocalAppPalette.current.background)) {
        SmallBackTopAppBar("Settings", onBack)
        CatalogDivider()
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = Dimens.xl)) {
            item {
                AppearanceSettingsSection(
                    state = state,
                    wallpaperSupported = wallpaperSupported,
                    onThemeSelect = viewModel::setThemeMode,
                    onDynamicColorChange = viewModel::setDynamicColor,
                )
                Spacer(Modifier.height(Dimens.lg))
                CatalogDivider()
                CatalogSettingsSection(
                    state = state,
                    updated = updated,
                    onRefresh = viewModel::refresh,
                )
                Spacer(Modifier.height(Dimens.lg))
                CatalogDivider()
                AboutSettingsSection(version = version)
            }
        }
    }
}

@Composable
private fun AppearanceSettingsSection(
    state: SettingsUiState,
    wallpaperSupported: Boolean,
    onThemeSelect: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
) {
    SectionHeader("Appearance")
    Column(
        Modifier.padding(horizontal = Dimens.lg),
        verticalArrangement = Arrangement.spacedBy(Dimens.sm),
    ) {
        Text("Theme", style = MaterialTheme.typography.bodyMedium)
        SingleChoiceButtonGroup(
            labels = ThemeMode.entries.map { it.name },
            selectedIndex = ThemeMode.entries.indexOf(state.settings.themeMode),
            onSelect = { onThemeSelect(ThemeMode.entries[it]) },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Use wallpaper colors",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
            )
            Switch(
                checked = state.settings.dynamicColor,
                onCheckedChange = onDynamicColorChange,
                enabled = wallpaperSupported,
                modifier =
                    Modifier.semantics { contentDescription = "Use wallpaper colors" },
            )
        }
        if (!wallpaperSupported) {
            Text(
                "Requires Android 12 or later.",
                style = MaterialTheme.typography.bodySmall,
                color = LocalAppPalette.current.textSecondary,
            )
        }
        state.error?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun CatalogSettingsSection(
    state: SettingsUiState,
    updated: String,
    onRefresh: () -> Unit,
) {
    SectionHeader("Catalog")
    Column(
        Modifier.padding(horizontal = Dimens.lg),
        verticalArrangement = Arrangement.spacedBy(Dimens.sm),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Last updated: $updated",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(
                onClick = onRefresh,
                enabled = state.syncState !is SyncState.Running,
                shape = MaterialTheme.shapes.small,
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text("Sync now")
            }
        }
        Text(
            when (val sync = state.syncState) {
                SyncState.Idle ->
                    if (state.lastSyncedAt == null) "Ready to sync"
                    else "Catalog can be refreshed"
                SyncState.Running -> "Refreshing catalog…"
                is SyncState.Success -> "Catalog is up to date"
                is SyncState.Failed -> "Couldn't refresh: ${sync.message}"
            },
            style = MaterialTheme.typography.bodySmall,
            color = LocalAppPalette.current.textSecondary,
        )
        Text(
            "The catalog refreshes daily when you're connected. " +
                "Tap Sync now or pull down to refresh at any time.",
            style = MaterialTheme.typography.bodySmall,
            color = LocalAppPalette.current.textSecondary,
        )
    }
}

@Composable
private fun AboutSettingsSection(version: String) {
    SectionHeader("About")
    Column(
        Modifier.padding(horizontal = Dimens.lg),
        verticalArrangement = Arrangement.spacedBy(Dimens.sm),
    ) {
        Text("ModeLLM $version", style = MonoStyle)
        val linkColor = LocalAppPalette.current.brandText
        Text(
            modelsDevAnnotatedString(linkColor),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            fontsLicenseAnnotatedString(linkColor),
            style = MaterialTheme.typography.bodySmall,
            color = LocalAppPalette.current.textSecondary,
        )
    }
}

private fun modelsDevAnnotatedString(linkColor: Color): AnnotatedString =
    buildAnnotatedString {
        append("Model data and provider logos come from ")
        withLink(
            LinkAnnotation.Url(
                ModelsDevUrl,
                TextLinkStyles(
                    style =
                        SpanStyle(
                            color = linkColor,
                            textDecoration = TextDecoration.Underline,
                        )
                ),
            )
        ) {
            append("models.dev")
        }
        append(".")
    }

private fun fontsLicenseAnnotatedString(linkColor: Color): AnnotatedString =
    buildAnnotatedString {
        withLink(
            LinkAnnotation.Url(
                RubikLicenseUrl,
                TextLinkStyles(
                    style =
                        SpanStyle(
                            color = linkColor,
                            textDecoration = TextDecoration.Underline,
                        )
                ),
            )
        ) {
            append("Rubik")
        }
        append(" and ")
        withLink(
            LinkAnnotation.Url(
                IbmPlexMonoLicenseUrl,
                TextLinkStyles(
                    style =
                        SpanStyle(
                            color = linkColor,
                            textDecoration = TextDecoration.Underline,
                        )
                ),
            )
        ) {
            append("IBM Plex Mono")
        }
        append(" are licensed under the SIL Open Font License (OFL).")
    }
