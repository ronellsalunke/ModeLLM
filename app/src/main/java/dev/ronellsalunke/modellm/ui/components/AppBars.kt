package dev.ronellsalunke.modellm.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextOverflow
import dev.ronellsalunke.modellm.ui.theme.LocalAppPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopAppBar(
    title: String,
    leading: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = { leading?.invoke() },
        actions = actions,
        windowInsets = WindowInsets(0, 0, 0, 0),
        colors = appTopBarColors(),
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DetailTopAppBar(
    title: String,
    onBack: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    MediumFlexibleTopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = { BackIconButton(onBack) },
        scrollBehavior = scrollBehavior,
        windowInsets = WindowInsets(0, 0, 0, 0),
        colors = appTopBarColors(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmallBackTopAppBar(title: String, onBack: () -> Unit) {
    TopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = { BackIconButton(onBack) },
        windowInsets = WindowInsets(0, 0, 0, 0),
        colors = appTopBarColors(),
    )
}

@Composable
fun BackIconButton(onBack: () -> Unit) {
    FilledTonalIconButton(onClick = onBack) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun appTopBarColors(): TopAppBarColors =
    TopAppBarDefaults.topAppBarColors(
        containerColor = LocalAppPalette.current.background,
        scrolledContainerColor = LocalAppPalette.current.background,
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberDetailScrollBehavior(): TopAppBarScrollBehavior =
    TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
