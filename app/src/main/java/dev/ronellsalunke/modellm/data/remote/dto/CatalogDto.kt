package dev.ronellsalunke.modellm.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * The root of `api.json` is a JSON object keyed by provider id, so the response is
 * decoded as `Map<String, ProviderDto>`. Every field is nullable because models.dev
 * adds, renames, and omits fields without a schema version.
 */
@Serializable
data class ProviderDto(
    val id: String? = null,
    val name: String? = null,
    val env: List<String>? = null,
    val npm: String? = null,
    val doc: String? = null,
    val api: String? = null,
    val models: Map<String, ModelDto>? = null,
)

@Serializable
data class ModelDto(
    val id: String? = null,
    val name: String? = null,
    val description: String? = null,
    val attachment: Boolean? = null,
    val reasoning: Boolean? = null,
    @SerialName("tool_call") val toolCall: Boolean? = null,
    @SerialName("last_updated") val lastUpdated: String? = null,
    val limit: LimitDto? = null,
    val modalities: ModalitiesDto? = null,
    @SerialName("open_weights") val openWeights: Boolean? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    val family: String? = null,
    val cost: CostDto? = null,
    val temperature: Boolean? = null,
    @SerialName("structured_output") val structuredOutput: Boolean? = null,
    @SerialName("canonical_model_id") val canonicalModelId: String? = null,
    @SerialName("reasoning_options") val reasoningOptions: List<ReasoningOptionDto>? = null,
    val knowledge: String? = null,
    val provider: JsonElement? = null,
    val interleaved: JsonElement? = null,
    val status: String? = null,
    val experimental: JsonElement? = null,
    val type: String? = null,
)

@Serializable
data class LimitDto(
    val context: Int? = null,
    val input: Int? = null,
    val output: Int? = null,
)

@Serializable
data class ModalitiesDto(
    val input: List<String>? = null,
    val output: List<String>? = null,
)

@Serializable
data class CostDto(
    val input: Double? = null,
    val output: Double? = null,
    @SerialName("cache_read") val cacheRead: Double? = null,
    @SerialName("cache_write") val cacheWrite: Double? = null,
    val reasoning: Double? = null,
    @SerialName("input_audio") val inputAudio: Double? = null,
    @SerialName("output_audio") val outputAudio: Double? = null,
)

@Serializable
data class ReasoningOptionDto(
    val type: String? = null,
    val values: List<String?>? = null,
    val min: Long? = null,
    val max: Long? = null,
)
