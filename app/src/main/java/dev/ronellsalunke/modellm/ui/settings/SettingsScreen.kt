package dev.ronellsalunke.modellm.ui.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.Icons
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ronellsalunke.modellm.data.repository.SyncState
import dev.ronellsalunke.modellm.data.settings.ThemeMode
import dev.ronellsalunke.modellm.ui.components.CatalogDivider
import dev.ronellsalunke.modellm.ui.components.AppSwitch
import dev.ronellsalunke.modellm.ui.components.rememberExternalLinkOpener
import dev.ronellsalunke.modellm.ui.components.rememberRelativeTime
import dev.ronellsalunke.modellm.ui.components.SectionHeader
import dev.ronellsalunke.modellm.ui.components.SingleChoiceButtonGroup
import dev.ronellsalunke.modellm.ui.components.SmallBackTopAppBar
import dev.ronellsalunke.modellm.ui.components.safeDrawingBottomPadding
import dev.ronellsalunke.modellm.ui.components.navigationAreaBottomPadding
import dev.ronellsalunke.modellm.ui.components.safeDrawingHorizontalPadding
import dev.ronellsalunke.modellm.ui.theme.Dimens
import dev.ronellsalunke.modellm.ui.theme.LocalAppPalette
import dev.ronellsalunke.modellm.ui.theme.MonoStyle
import org.koin.androidx.compose.koinViewModel

private const val ModelsDevUrl = "https://models.dev"
private const val AppGithubUrl = "https://github.com/ronellsalunke/ModeLLM"
private val SettingsListItemMinHeight = 56.dp

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLicenses: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val version =
        remember(context) {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "–"
        }
    val updated = rememberRelativeTime(state.lastSyncedAt)
    val wallpaperSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    Column(
        Modifier.fillMaxSize()
            .background(LocalAppPalette.current.background)
            .safeDrawingHorizontalPadding()
    ) {
        SmallBackTopAppBar("Settings", onBack)
        CatalogDivider()
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(
                bottom = Dimens.xl + safeDrawingBottomPadding() + navigationAreaBottomPadding(),
            ),
        ) {
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
                AboutSettingsSection(version = version, onLicenses = onLicenses)
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
        SettingsTileGroup {
            SettingsListItem(
                trailingContent = {
                    AppSwitch(
                        checked = state.settings.dynamicColor,
                        onCheckedChange = onDynamicColorChange,
                        enabled = wallpaperSupported,
                        modifier =
                            Modifier.semantics {
                                contentDescription = "Use wallpaper colors"
                            },
                    )
                },
                supportingContent =
                    if (wallpaperSupported) {
                        null
                    } else {
                        {
                            Text(
                                "Requires Android 12 or later.",
                                style = MaterialTheme.typography.bodySmall,
                                color = LocalAppPalette.current.textSecondary,
                            )
                        }
                    },
            ) {
                Text("Use wallpaper colors")
            }
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
private fun SettingsTileGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.clip(MaterialTheme.shapes.small)
            .border(
                Dimens.hairline,
                LocalAppPalette.current.border,
                MaterialTheme.shapes.small,
            ),
        content = content,
    )
}

@Composable
private fun SettingsListItem(
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    shape: Shape = RectangleShape,
    supportingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    headlineContent: @Composable () -> Unit,
) {
    val clickModifier =
        if (onClick != null) {
            Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        } else {
            Modifier
        }
    Row(
        Modifier.fillMaxWidth()
            .heightIn(min = SettingsListItemMinHeight)
            .clip(shape)
            .then(clickModifier)
            .padding(horizontal = Dimens.lg, vertical = Dimens.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Dimens.xxs),
        ) {
            headlineContent()
            if (supportingContent != null) {
                supportingContent()
            }
        }
        if (trailingContent != null) {
            Spacer(Modifier.width(Dimens.lg))
            trailingContent()
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
    Column(Modifier.padding(horizontal = Dimens.lg)) {
        SettingsTileGroup {
            SettingsListItem(
                onClick = onRefresh,
                enabled = state.syncState !is SyncState.Running,
                shape = MaterialTheme.shapes.small,
                trailingContent = {
                    if (state.syncState is SyncState.Running) {
                        CircularProgressIndicator(
                            modifier =
                                Modifier.size(Dimens.iconMedium)
                                    .semantics {
                                        contentDescription = "Refreshing catalog"
                                    },
                            strokeWidth = Dimens.xxs,
                        )
                    } else {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Sync now",
                            tint = LocalAppPalette.current.textTertiary,
                        )
                    }
                },
            ) {
                Text("Last updated: $updated")
            }
        }
    }
}

@Composable
private fun AboutSettingsSection(version: String, onLicenses: () -> Unit) {
    val openLink = rememberExternalLinkOpener()
    SectionHeader("About")
    Column(Modifier.padding(horizontal = Dimens.lg)) {
        SettingsTileGroup {
            SettingsListItem(
                trailingContent = {
                    Text(
                        version,
                        style = MonoStyle,
                        color = LocalAppPalette.current.textSecondary,
                    )
                },
            ) {
                Text("Version")
            }
            CatalogDivider()
            SettingsListItem(
                onClick = onLicenses,
                trailingContent = {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = LocalAppPalette.current.textTertiary,
                    )
                },
            ) {
                Text("Open source licenses")
            }
            CatalogDivider()
            SettingsListItem(
                onClick = { openLink(AppGithubUrl) },
                trailingContent = {
                    Icon(
                        Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = LocalAppPalette.current.textTertiary,
                    )
                },
            ) {
                Text("Source code")
            }
            CatalogDivider()
            SettingsListItem(
                onClick = { openLink(ModelsDevUrl) },
                shape = RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp),
                trailingContent = {
                    Icon(
                        Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = LocalAppPalette.current.textTertiary,
                    )
                },
            ) {
                Text("models.dev")
            }
        }
    }
}
