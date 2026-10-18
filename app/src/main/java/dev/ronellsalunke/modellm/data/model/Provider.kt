package dev.ronellsalunke.modellm.data.model

data class Provider(
    val id: String,
    val name: String,
    val env: List<String>,
    val npm: String?,
    val doc: String?,
    val api: String?,
    val modelCount: Int,
)
