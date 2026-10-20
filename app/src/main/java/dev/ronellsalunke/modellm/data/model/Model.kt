package dev.ronellsalunke.modellm.data.model

import kotlinx.serialization.Serializable

enum class Modality(val wireName: String) {
    Text("text"),
    Image("image"),
    Pdf("pdf"),
    Audio("audio"),
    Video("video");

    companion object {
        fun fromWireName(value: String): Modality? = entries.firstOrNull {
            it.wireName.equals(value, ignoreCase = true)
        }
    }
}

enum class Capability {
    Reasoning,
    ToolCall,
    StructuredOutput,
    Attachment,
    Temperature,
}

enum class ModelSort {
    Newest,
    Cheapest,
    LargestContext,
    Name,
}

data class ModelFilters(
    val providerIds: Set<String> = emptySet(),
    val inputModalities: Set<Modality> = emptySet(),
    val capabilities: Set<Capability> = emptySet(),
    val openWeights: Boolean? = null,
    val statuses: Set<String> = emptySet(),
    val knowledgeAfter: String? = null,
    val knowledgeBefore: String? = null,
    val labIds: Set<String> = emptySet(),
)

data class ModelLimits(
    val context: Int?,
    val input: Int?,
    val output: Int?,
)

data class ModelCost(
    val input: Double?,
    val output: Double?,
    val cacheRead: Double?,
    val cacheWrite: Double?,
    val reasoning: Double?,
    val inputAudio: Double?,
    val outputAudio: Double?,
)

@Serializable
data class ReasoningOption(
    val type: String? = null,
    val values: List<String?>? = null,
    val min: Long? = null,
    val max: Long? = null,
)

data class Model(
    val providerId: String,
    val providerName: String,
    val id: String,
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
    val limits: ModelLimits,
    val cost: ModelCost,
    val inputModalities: List<Modality>,
    val outputModalities: List<Modality>,
    val reasoningOptions: List<ReasoningOption>,
    val interleavedJson: String?,
    val providerNpm: String?,
) {
    val supportsInterleaved: Boolean
        get() = interleavedJson != null && interleavedJson != "false" && interleavedJson != "null"
}
