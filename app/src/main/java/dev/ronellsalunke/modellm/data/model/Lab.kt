package dev.ronellsalunke.modellm.data.model

data class Lab(
    val id: String,
    val name: String,
    val description: String?,
    val modelCount: Int,
    val providerCount: Int,
    val lastUpdated: String?,
)
