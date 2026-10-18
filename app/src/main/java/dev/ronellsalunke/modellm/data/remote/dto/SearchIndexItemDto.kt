package dev.ronellsalunke.modellm.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SearchIndexItemDto(
    val type: String? = null,
    val title: String? = null,
    val id: String? = null,
    val href: String? = null,
    val logo: String? = null,
    val tokens: List<String>? = null,
    val lab: String? = null,
    val modelCount: Int? = null,
    val providerCount: Int? = null,
    val context: Int? = null,
    val releaseDate: String? = null,
    val inputCost: Double? = null,
    val outputCost: Double? = null,
    val description: String? = null,
    val npm: String? = null,
    val api: String? = null,
    val updated: String? = null,
)
