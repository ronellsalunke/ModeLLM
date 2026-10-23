package dev.ronellsalunke.modellm.ui.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import dev.ronellsalunke.modellm.ui.theme.LocalAppPalette

@Composable
fun HomeTopAppBar(
    title: String,
    leading: (@Composable () -> Unit)? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = { leading?.invoke() },
        actions = actions,
        scrollBehavior = scrollBehavior,
        modifier = Modifier.background(LocalAppPalette.current.background),
        colors = appTopBarColors(),
    )
}

@Composable
fun DetailTopAppBar(
    title: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = { if (LocalDetailShowsBackButton.current) BackIconButton(onBack) },
        actions = actions,
        modifier = Modifier.background(LocalAppPalette.current.background),
        colors = appTopBarColors(),
    )
}

@Composable
fun SmallBackTopAppBar(title: String, onBack: () -> Unit) {
    TopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
            if (LocalDetailShowsBackButton.current) BackIconButton(onBack)
        },
        modifier = Modifier.background(LocalAppPalette.current.background),
        colors = appTopBarColors(),
    )
}

@Composable
fun BackIconButton(onBack: () -> Unit) {
    TooltipIconButton(text = "Back", onClick = onBack) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
    }
}

@Composable
fun ShareIconButton(url: String) {
    val context = LocalContext.current
    TooltipIconButton(
        text = "Share",
        onClick = {
            val send =
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, url)
                }
            context.startActivity(Intent.createChooser(send, null))
        },
    ) {
        Icon(Icons.Default.Share, contentDescription = "Share")
    }
}

@Composable
private fun appTopBarColors(): TopAppBarColors =
    TopAppBarDefaults.topAppBarColors(
        containerColor = Color.Transparent,
        scrolledContainerColor = Color.Transparent,
    )
