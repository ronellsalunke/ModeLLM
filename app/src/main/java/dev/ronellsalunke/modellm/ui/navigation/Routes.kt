package dev.ronellsalunke.modellm.ui.navigation

import kotlinx.serialization.Serializable

@Serializable data object TabsRoute
@Serializable data object SettingsRoute
@Serializable data class CanonicalModelDetailRoute(val canonicalId: String)
@Serializable data class ProviderModelDetailRoute(val providerId: String, val modelId: String)
@Serializable data class ProviderDetailRoute(val providerId: String)
@Serializable data class LabDetailRoute(val labId: String)
