package dev.ronellsalunke.modellm.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object TabsRoute : NavKey

@Serializable data object SettingsRoute : NavKey

@Serializable data class CanonicalModelDetailRoute(val canonicalId: String) : NavKey

@Serializable
data class ProviderModelDetailRoute(val providerId: String, val modelId: String) : NavKey

@Serializable data class ProviderDetailRoute(val providerId: String) : NavKey

@Serializable data class LabDetailRoute(val labId: String) : NavKey
