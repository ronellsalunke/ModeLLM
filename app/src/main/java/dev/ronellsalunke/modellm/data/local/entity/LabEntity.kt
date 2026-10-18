package dev.ronellsalunke.modellm.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "labs")
data class LabEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String?,
    val modelCount: Int,
    val providerCount: Int,
    val lastUpdated: String?,
)
