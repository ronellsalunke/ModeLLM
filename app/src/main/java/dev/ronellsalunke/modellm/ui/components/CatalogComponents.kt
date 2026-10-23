package dev.ronellsalunke.modellm.ui.components

import android.content.ActivityNotFoundException
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import dev.ronellsalunke.modellm.ui.theme.Dimens
import dev.ronellsalunke.modellm.ui.theme.LocalAppPalette

/** Material text-field height, which leaves room around the trailing icon's state layer. */
private val SearchFieldHeight = 56.dp

@Composable
fun CatalogDivider() =
    HorizontalDivider(thickness = Dimens.hairline, color = LocalAppPalette.current.border)

@Composable
fun SectionHeader(title: String) {
    Text(
        title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = LocalAppPalette.current.textTertiary,
        modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.lg, vertical = Dimens.md),
    )
}

@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
) {
    val palette = LocalAppPalette.current
    val dismissFocus = dismissCatalogFocus()
    Row(
        modifier
            .heightIn(min = SearchFieldHeight)
            .border(1.dp, palette.border, MaterialTheme.shapes.small)
            .background(palette.surface, MaterialTheme.shapes.small)
            .padding(start = Dimens.md, end = Dimens.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = null,
            tint = palette.textTertiary,
            modifier = Modifier.size(20.dp),
        )
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle =
                MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
            cursorBrush = SolidColor(palette.brand),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { dismissFocus() }),
            modifier =
                Modifier.weight(1f)
                    .focusRequester(focusRequester)
                    .dismissSearchOnEscape(dismissFocus)
                    .heightIn(min = SearchFieldHeight)
                    .padding(horizontal = Dimens.sm)
                    .semantics { contentDescription = label },
            decorationBox = { field ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(
                            label,
                            color = palette.textTertiary,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    field()
                }
            },
        )
        if (query.isNotEmpty()) {
            SearchClearButton(
                onClick = {
                    onQueryChange("")
                    dismissFocus()
                }
            )
        }
    }
}

@Composable
private fun SearchClearButton(onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
    ) {
        Icon(
            Icons.Default.Close,
            contentDescription = "Clear search",
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
fun ExternalLink(label: String, url: String, modifier: Modifier = Modifier) {
    val handler = LocalUriHandler.current
    val context = LocalContext.current
    TextButton(
        onClick = {
            try {
                val scheme = url.toUri().scheme
                if (scheme != "https" && scheme != "http") {
                    throw IllegalArgumentException("Unsupported URL scheme: $scheme")
                }
                handler.openUri(url)
            } catch (_: IllegalArgumentException) {
                Toast.makeText(context, "Couldn't open this link", Toast.LENGTH_SHORT).show()
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(context, "No app can open this link", Toast.LENGTH_SHORT).show()
            }
        },
        modifier = modifier.heightIn(min = 48.dp).pointerHoverIcon(PointerIcon.Hand),
        shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.textButtonColors(contentColor = LocalAppPalette.current.brandText),
        contentPadding = PaddingValues(horizontal = 4.dp),
    ) {
        Icon(
            Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(label, textDecoration = TextDecoration.Underline)
    }
}
