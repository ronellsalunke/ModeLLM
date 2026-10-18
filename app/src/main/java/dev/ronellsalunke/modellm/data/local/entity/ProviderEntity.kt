package dev.ronellsalunke.modellm.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "providers")
data class ProviderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val env: List<String>,
    val npm: String?,
    val doc: String?,
    val api: String?,
)
