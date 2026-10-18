package dev.ronellsalunke.modellm.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "canonical_model_providers",
    primaryKeys = ["canonicalId", "providerId", "providerModelId"],
    indices = [Index("canonicalId"), Index("providerId")],
    foreignKeys = [
        ForeignKey(
            entity = CanonicalModelEntity::class,
            parentColumns = ["canonicalId"],
            childColumns = ["canonicalId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ProviderEntity::class,
            parentColumns = ["id"],
            childColumns = ["providerId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class CanonicalProviderEntity(
    val canonicalId: String,
    val providerId: String,
    val providerModelId: String,
)
