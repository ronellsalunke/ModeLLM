package dev.ronellsalunke.modellm.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "canonical_models",
    indices =
        [
            Index(value = ["canonicalId"], unique = true),
            Index("releaseDate"),
            Index("minInputCost"),
            Index("contextLimit"),
            Index("knowledge"),
            Index("labId"),
        ],
)
data class CanonicalModelEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val canonicalId: String,
    val name: String,
    val description: String?,
    val family: String?,
    val labId: String,
    val labName: String,
    val providerNames: String,
    val providerCount: Int,
    val attachment: Boolean,
    val reasoning: Boolean,
    val toolCall: Boolean,
    val structuredOutput: Boolean,
    val temperature: Boolean,
    val openWeights: Boolean,
    val releaseDate: String?,
    val lastUpdated: String?,
    val knowledge: String?,
    val modelType: String?,
    val contextLimit: Int?,
    val maxOutputTokens: Int?,
    val minInputCost: Double?,
    val minOutputCost: Double?,
    val supportsTextInput: Boolean,
    val supportsImageInput: Boolean,
    val supportsPdfInput: Boolean,
    val supportsAudioInput: Boolean,
    val supportsVideoInput: Boolean,
    val supportsTextOutput: Boolean,
    val supportsImageOutput: Boolean,
    val supportsPdfOutput: Boolean,
    val supportsAudioOutput: Boolean,
    val supportsVideoOutput: Boolean,
)
