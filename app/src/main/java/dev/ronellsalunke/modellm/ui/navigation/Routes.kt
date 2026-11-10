package dev.ronellsalunke.modellm.ui.navigation

import androidx.navigation3.runtime.NavKey
import dev.ronellsalunke.modellm.ui.util.ModelsDevLinks
import kotlinx.serialization.Serializable

@Serializable data object TabsRoute : NavKey

@Serializable data object SettingsRoute : NavKey

@Serializable data object LicensesRoute : NavKey

@Serializable data class CanonicalModelDetailRoute(val canonicalId: String) : NavKey

@Serializable
data class ProviderModelDetailRoute(
    val providerId: String,
    val modelId: String,
    val canonicalModelId: String? = null,
) : NavKey

@Serializable data class ProviderDetailRoute(val providerId: String) : NavKey

@Serializable data class LabDetailRoute(val labId: String) : NavKey

/** The models.dev page for the destination, or null when the destination has no public page. */
fun NavKey.assistWebUri(): String? =
    when (this) {
        is CanonicalModelDetailRoute -> ModelsDevLinks.model(canonicalId)
        is ProviderModelDetailRoute ->
            canonicalModelId?.let(ModelsDevLinks::model) ?: ModelsDevLinks.provider(providerId)
        is ProviderDetailRoute -> ModelsDevLinks.provider(providerId)
        is LabDetailRoute -> ModelsDevLinks.lab(labId)
        else -> null
    }
