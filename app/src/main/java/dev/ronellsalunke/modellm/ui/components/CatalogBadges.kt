package dev.ronellsalunke.modellm.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.ronellsalunke.modellm.data.model.Capability
import dev.ronellsalunke.modellm.data.model.Modality
import dev.ronellsalunke.modellm.ui.theme.LocalAppPalette
import dev.ronellsalunke.modellm.ui.theme.MonoLabelStyle
import dev.ronellsalunke.modellm.ui.util.capabilityLabel
import dev.ronellsalunke.modellm.ui.util.modalityLabel

@Composable
fun ProviderLogo(providerId: String, name: String, size: Dp = 24.dp) {
    CatalogLogo("https://models.dev/logos/$providerId.svg", name, size)
}

@Composable
fun LabLogo(labId: String, name: String, size: Dp = 24.dp) {
    CatalogLogo("https://models.dev/logos/labs/$labId.svg", name, size)
}

@Composable
private fun CatalogLogo(url: String, name: String, size: Dp) {
    var loaded by remember(url) { mutableStateOf(false) }
    Box(
        Modifier.size(size).semantics(mergeDescendants = true) {
            contentDescription = "$name logo"
        },
        contentAlignment = Alignment.Center,
    ) {
        if (!loaded) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    name.take(2).uppercase(),
                    style = MonoLabelStyle,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        AsyncImage(
            model = url,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
            onLoading = { loaded = false },
            onError = { loaded = false },
            onSuccess = { loaded = true },
        )
    }
}

@Composable
private fun LabelTile(label: String, modifier: Modifier = Modifier) {
    Text(
        label.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
        color = LocalAppPalette.current.textSecondary,
        modifier =
            modifier
                .border(1.dp, LocalAppPalette.current.border, MaterialTheme.shapes.small)
                .background(LocalAppPalette.current.surface, MaterialTheme.shapes.small)
                .padding(horizontal = 6.dp, vertical = 3.dp),
    )
}

@Composable
fun CapabilityBadge(capability: Capability) = LabelTile(capabilityLabel(capability))

@Composable
fun StatusChip(status: String) {
    val deprecated = status.equals("deprecated", ignoreCase = true)
    val palette = LocalAppPalette.current
    val color = if (deprecated) MaterialTheme.colorScheme.onErrorContainer else palette.brandText
    val background =
        if (deprecated) MaterialTheme.colorScheme.errorContainer
        else palette.brand.copy(alpha = 0.12f)
    Text(
        status.replaceFirstChar { it.uppercase() },
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
        color = color,
        modifier =
            Modifier.background(background, RoundedCornerShape(4.dp))
                .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp, vertical = 1.dp),
    )
}

@Composable
fun ModalityTile(modality: Modality) = LabelTile(modalityLabel(modality))
