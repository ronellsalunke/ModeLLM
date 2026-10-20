package dev.ronellsalunke.modellm.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.ronellsalunke.modellm.data.repository.SyncState
import dev.ronellsalunke.modellm.ui.theme.Dimens
import dev.ronellsalunke.modellm.ui.theme.LocalAppPalette
import dev.ronellsalunke.modellm.ui.util.relativeTime
import kotlinx.coroutines.delay

@Composable
fun rememberRelativeTime(timestamp: Long?): String {
    val now by
        produceState(System.currentTimeMillis(), timestamp) {
            while (true) {
                value = System.currentTimeMillis()
                delay(60_000)
            }
        }
    return timestamp?.let { relativeTime(it, now) } ?: "Never"
}

@Composable
fun SyncBanner(lastSyncedAt: Long?, state: SyncState, onRetry: () -> Unit) {
    val time = rememberRelativeTime(lastSyncedAt)
    if (lastSyncedAt != null || state is SyncState.Failed || state is SyncState.Running) {
        Column(
            Modifier.fillMaxWidth()
                .background(LocalAppPalette.current.surface)
                .padding(horizontal = Dimens.lg)
        ) {
            if (lastSyncedAt != null) {
                Text(
                    "Updated $time",
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalAppPalette.current.textSecondary,
                    modifier = Modifier.padding(vertical = Dimens.sm),
                )
            }
            if (state is SyncState.Running) {
                Text(
                    "Refreshing catalog…",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = Dimens.sm),
                )
            }
            if (state is SyncState.Failed) {
                Text(
                    if (lastSyncedAt != null) "Couldn't refresh. Showing saved catalog."
                    else "Couldn't download the catalog.",
                    style = MaterialTheme.typography.bodySmall,
                )
                TextButton(
                    onClick = onRetry,
                    colors =
                        ButtonDefaults.textButtonColors(
                            contentColor = LocalAppPalette.current.brandText
                        ),
                ) {
                    Text("Retry")
                }
            }
        }
        CatalogDivider()
    }
}

@Composable
fun EmptyState(
    title: String,
    message: String? = null,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    Column(
        Modifier.fillMaxWidth().padding(Dimens.xl),
        verticalArrangement = Arrangement.spacedBy(Dimens.md),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        message?.let {
            Text(
                it,
                color = LocalAppPalette.current.textSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        actionLabel?.let {
            OutlinedButton(
                onClick = onAction,
                shape = MaterialTheme.shapes.small,
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        contentColor = LocalAppPalette.current.brandText
                    ),
                border = BorderStroke(1.dp, LocalAppPalette.current.border),
            ) {
                Text(it)
            }
        }
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
