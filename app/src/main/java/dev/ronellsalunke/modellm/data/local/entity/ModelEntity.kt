package dev.ronellsalunke.modellm.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "models",
    foreignKeys = [
        ForeignKey(
            entity = ProviderEntity::class,
            parentColumns = ["id"],
            childColumns = ["providerId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("modelId"),
        Index(value = ["providerId", "modelId"], unique = true),
        Index("releaseDate"),
        Index("costInput"),
        Index("contextLimit"),
        Index("knowledge"),
    ],
)
data class ModelEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val providerId: String,
    val modelId: String,
    val name: String,
    val description: String?,
    val family: String?,
    val attachment: Boolean,
    val reasoning: Boolean,
    val toolCall: Boolean,
    val structuredOutput: Boolean,
    val temperature: Boolean,
    val openWeights: Boolean,
    val status: String?,
    val experimental: Boolean,
    val modelType: String?,
    val canonicalModelId: String?,
    val releaseDate: String?,
    val lastUpdated: String?,
    val knowledge: String?,
    val contextLimit: Int?,
    val maxInputTokens: Int?,
    val maxOutputTokens: Int?,
    val costInput: Double?,
    val costOutput: Double?,
    val costCacheRead: Double?,
    val costCacheWrite: Double?,
    val costReasoning: Double?,
    val costInputAudio: Double?,
    val costOutputAudio: Double?,
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
    val reasoningOptionsJson: String?,
    val interleavedJson: String?,
    val providerNpm: String?,
)
