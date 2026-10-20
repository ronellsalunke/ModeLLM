package dev.ronellsalunke.modellm.data.remote

import kotlinx.serialization.json.Json

/**
 * Shared lenient configuration for the models.dev payload. The catalogue is a living dataset, so
 * unknown keys and missing values must never break a sync.
 */
val CatalogJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
    explicitNulls = false
}
