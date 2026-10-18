package dev.ronellsalunke.modellm.data.mapper

import dev.ronellsalunke.modellm.data.local.dao.ModelWithProvider
import dev.ronellsalunke.modellm.data.local.dao.ProviderWithCount
import dev.ronellsalunke.modellm.data.local.entity.ModelEntity
import dev.ronellsalunke.modellm.data.local.entity.ModelSearchEntity
import dev.ronellsalunke.modellm.data.local.entity.ProviderEntity
import dev.ronellsalunke.modellm.data.model.Model
import dev.ronellsalunke.modellm.data.model.ModelCost
import dev.ronellsalunke.modellm.data.model.ModelLimits
import dev.ronellsalunke.modellm.data.model.Modality
import dev.ronellsalunke.modellm.data.model.Provider
import dev.ronellsalunke.modellm.data.model.ReasoningOption
import dev.ronellsalunke.modellm.data.remote.dto.ModelDto
import dev.ronellsalunke.modellm.data.remote.dto.ProviderDto
import dev.ronellsalunke.modellm.data.remote.dto.ReasoningOptionDto
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

class CatalogMapper(private val json: Json) {

    fun toProviderEntity(dto: ProviderDto): ProviderEntity? {
        val id = dto.id?.takeIf { it.isNotBlank() } ?: return null
        return ProviderEntity(
            id = id,
            name = dto.name?.takeIf { it.isNotBlank() } ?: id,
            env = dto.env.orEmpty(),
            npm = dto.npm?.takeIf { it.isNotBlank() },
            doc = dto.doc?.takeIf { it.isNotBlank() },
            api = dto.api?.takeIf { it.isNotBlank() },
        )
    }

    fun toModelEntity(providerId: String, fallbackId: String, dto: ModelDto): ModelEntity {
        val modelId = dto.id?.takeIf { it.isNotBlank() } ?: fallbackId
        val inputModalities = dto.modalities?.input.orEmpty()
            .mapNotNull { Modality.fromWireName(it) }
            .toSet()
        val outputModalities = dto.modalities?.output.orEmpty()
            .mapNotNull { Modality.fromWireName(it) }
            .toSet()

        return ModelEntity(
            providerId = providerId,
            modelId = modelId,
            name = dto.name?.takeIf { it.isNotBlank() } ?: modelId,
            description = dto.description?.takeIf { it.isNotBlank() },
            family = dto.family?.takeIf { it.isNotBlank() },
            attachment = dto.attachment ?: false,
            reasoning = dto.reasoning ?: false,
            toolCall = dto.toolCall ?: false,
            structuredOutput = dto.structuredOutput ?: false,
            temperature = dto.temperature ?: false,
            openWeights = dto.openWeights ?: false,
            status = dto.status?.takeIf { it.isNotBlank() },
            experimental = dto.experimental.isExperimental(),
            modelType = dto.type?.takeIf { it.isNotBlank() },
            canonicalModelId = dto.canonicalModelId?.takeIf { it.isNotBlank() },
            releaseDate = dto.releaseDate?.takeIf { it.isNotBlank() },
            lastUpdated = dto.lastUpdated?.takeIf { it.isNotBlank() },
            knowledge = dto.knowledge?.takeIf { it.isNotBlank() },
            contextLimit = dto.limit?.context,
            maxInputTokens = dto.limit?.input,
            maxOutputTokens = dto.limit?.output,
            costInput = dto.cost?.input,
            costOutput = dto.cost?.output,
            costCacheRead = dto.cost?.cacheRead,
            costCacheWrite = dto.cost?.cacheWrite,
            costReasoning = dto.cost?.reasoning,
            costInputAudio = dto.cost?.inputAudio,
            costOutputAudio = dto.cost?.outputAudio,
            supportsTextInput = Modality.Text in inputModalities,
            supportsImageInput = Modality.Image in inputModalities,
            supportsPdfInput = Modality.Pdf in inputModalities,
            supportsAudioInput = Modality.Audio in inputModalities,
            supportsVideoInput = Modality.Video in inputModalities,
            supportsTextOutput = Modality.Text in outputModalities,
            supportsImageOutput = Modality.Image in outputModalities,
            supportsPdfOutput = Modality.Pdf in outputModalities,
            supportsAudioOutput = Modality.Audio in outputModalities,
            supportsVideoOutput = Modality.Video in outputModalities,
            reasoningOptionsJson = dto.reasoningOptions
                ?.takeIf { it.isNotEmpty() }
                ?.let { json.encodeToString(it) },
            interleavedJson = dto.interleaved?.toString(),
            providerNpm = dto.provider.providerNpm(),
        )
    }

    fun toSearchEntity(rowId: Long, entity: ModelEntity, providerName: String): ModelSearchEntity =
        ModelSearchEntity(
            rowId = rowId,
            providerId = entity.providerId,
            modelId = entity.modelId,
            name = entity.name,
            family = entity.family.orEmpty(),
            description = entity.description.orEmpty(),
            providerName = providerName,
        )

    fun toProvider(row: ProviderWithCount): Provider =
        Provider(
            id = row.provider.id,
            name = row.provider.name,
            env = row.provider.env,
            npm = row.provider.npm,
            doc = row.provider.doc,
            api = row.provider.api,
            modelCount = row.modelCount,
        )

    fun toModel(row: ModelWithProvider): Model {
        val entity = row.model
        val reasoningOptions = entity.reasoningOptionsJson
            ?.let { stored ->
                runCatching {
                    json.decodeFromString<List<ReasoningOptionDto>>(stored)
                }.getOrNull()
            }
            .orEmpty()
            .map { it.toDomain() }

        return Model(
            providerId = entity.providerId,
            providerName = row.providerName,
            id = entity.modelId,
            name = entity.name,
            description = entity.description,
            family = entity.family,
            attachment = entity.attachment,
            reasoning = entity.reasoning,
            toolCall = entity.toolCall,
            structuredOutput = entity.structuredOutput,
            temperature = entity.temperature,
            openWeights = entity.openWeights,
            status = entity.status,
            experimental = entity.experimental,
            modelType = entity.modelType,
            canonicalModelId = entity.canonicalModelId,
            releaseDate = entity.releaseDate,
            lastUpdated = entity.lastUpdated,
            knowledge = entity.knowledge,
            limits = ModelLimits(
                context = entity.contextLimit,
                input = entity.maxInputTokens,
                output = entity.maxOutputTokens,
            ),
            cost = ModelCost(
                input = entity.costInput,
                output = entity.costOutput,
                cacheRead = entity.costCacheRead,
                cacheWrite = entity.costCacheWrite,
                reasoning = entity.costReasoning,
                inputAudio = entity.costInputAudio,
                outputAudio = entity.costOutputAudio,
            ),
            inputModalities = buildList {
                if (entity.supportsTextInput) add(Modality.Text)
                if (entity.supportsImageInput) add(Modality.Image)
                if (entity.supportsPdfInput) add(Modality.Pdf)
                if (entity.supportsAudioInput) add(Modality.Audio)
                if (entity.supportsVideoInput) add(Modality.Video)
            },
            outputModalities = buildList {
                if (entity.supportsTextOutput) add(Modality.Text)
                if (entity.supportsImageOutput) add(Modality.Image)
                if (entity.supportsPdfOutput) add(Modality.Pdf)
                if (entity.supportsAudioOutput) add(Modality.Audio)
                if (entity.supportsVideoOutput) add(Modality.Video)
            },
            reasoningOptions = reasoningOptions,
            interleavedJson = entity.interleavedJson,
            providerNpm = entity.providerNpm,
        )
    }

    private fun ReasoningOptionDto.toDomain(): ReasoningOption =
        ReasoningOption(
            type = type,
            values = values,
            min = min,
            max = max,
        )

    private fun JsonElement?.providerNpm(): String? {
        val fields = this as? JsonObject ?: return null
        return (fields["npm"] as? JsonPrimitive)?.contentOrNull
    }

    private fun JsonElement?.isExperimental(): Boolean = when (this) {
        null -> false
        is JsonPrimitive -> booleanOrNull ?: true
        else -> true
    }
}
