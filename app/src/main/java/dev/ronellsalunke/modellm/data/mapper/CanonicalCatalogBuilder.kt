package dev.ronellsalunke.modellm.data.mapper

import dev.ronellsalunke.modellm.data.local.entity.CanonicalModelEntity
import dev.ronellsalunke.modellm.data.local.entity.CanonicalProviderEntity
import dev.ronellsalunke.modellm.data.local.entity.LabEntity
import dev.ronellsalunke.modellm.data.remote.CatalogPayload
import dev.ronellsalunke.modellm.data.remote.dto.ModelDto

data class CanonicalCatalogSnapshot(
    val models: List<CanonicalModelEntity>,
    val providers: List<CanonicalProviderEntity>,
    val labs: List<LabEntity>,
)

/** Builds canonical models, provider links, and labs from the three catalog endpoints. */
class CanonicalCatalogBuilder {
    fun build(payload: CatalogPayload): CanonicalCatalogSnapshot {
        val modelItems = payload.searchIndex
            .filter { it.type == "model" && !it.id.isNullOrBlank() }
            .associateBy { it.id!!.lowercase() }
        val labItems = payload.searchIndex
            .filter { it.type == "lab" && !it.id.isNullOrBlank() }
            .associateBy { it.id!!.lowercase() }
        val canonicalModels = payload.canonicalModels.entries
            .associateBy({ it.key.lowercase() }, { it.value })

        // One case-insensitive registry keeps models.json, the search index, and
        // provider declarations from producing duplicate canonical rows.
        val canonicalRegistry = LinkedHashMap<String, String>()
        (payload.canonicalModels.keys + payload.searchIndex
            .filter { it.type == "model" }
            .mapNotNull { it.id })
            .filter { it.isNotBlank() }
            .forEach { canonicalRegistry.putIfAbsent(it.lowercase(), it) }

        val providers = payload.providers.filter { !it.id.isNullOrBlank() }.associateBy { it.id!! }
        val variants = mutableMapOf<String, MutableList<Variant>>()

        fun resolveCanonicalId(id: String?): String? =
            id?.takeIf { it.isNotBlank() }?.let { canonicalRegistry[it.lowercase()] }

        fun registerCanonicalId(id: String): String =
            canonicalRegistry.getOrPut(id.lowercase()) { id }

        for ((providerId, provider) in providers) {
            for ((key, model) in provider.models.orEmpty()) {
                if (model.status == "alpha") continue
                val modelId = model.id?.takeIf { it.isNotBlank() } ?: key
                if (modelId.isBlank()) continue
                val declaredId = model.canonicalModelId?.takeIf { it.isNotBlank() }
                val canonicalId = resolveCanonicalId(declaredId)
                    ?: resolveCanonicalId(key)
                    ?: resolveCanonicalId("$providerId/$key")
                    ?: declaredId?.let(::registerCanonicalId)
                    ?: continue
                variants.getOrPut(canonicalId) { mutableListOf() }
                    .add(Variant(providerId, modelId, model))
            }
        }

        fun labName(id: String): String = labItems[id.lowercase()]?.title
            ?: LAB_NAME_OVERRIDES[id.lowercase()]
            ?: providers[id.lowercase()]?.name
            ?: id.split('-').joinToString(" ") { it.replaceFirstChar(Char::uppercaseChar) }

        val links = variants.flatMap { (id, entries) ->
            entries.map { CanonicalProviderEntity(id, it.providerId, it.modelId) }
        }.distinct()
        val linksByCanonicalId = links.groupBy { it.canonicalId }

        val models = canonicalRegistry.values.map { id ->
            val base = canonicalModels[id.lowercase()]
            val entries = variants[id].orEmpty()
            val variant = entries.firstOrNull()?.model
            val item = modelItems[id.lowercase()]
            val labId = (
                id.substringBefore('/', "").takeIf { id.contains('/') && it.isNotBlank() }
                    ?: item?.lab
                    ?: entries.firstOrNull()?.providerId
                    ?: id
                ).lowercase()
            val input = (base?.modalities?.input ?: variant?.modalities?.input).orEmpty()
            val output = (base?.modalities?.output ?: variant?.modalities?.output).orEmpty()

            CanonicalModelEntity(
                canonicalId = id,
                name = base?.name ?: item?.title ?: variant?.name ?: id,
                description = base?.description ?: item?.description ?: variant?.description,
                family = base?.family ?: variant?.family,
                labId = labId,
                labName = labName(labId),
                providerNames = entries.map { providers[it.providerId]?.name ?: it.providerId }
                    .distinct().joinToString(" "),
                providerCount = if (entries.isNotEmpty()) {
                    linksByCanonicalId[id].orEmpty().size
                } else {
                    item?.providerCount?.coerceAtLeast(0) ?: 0
                },
                attachment = base?.attachment ?: variant?.attachment ?: false,
                reasoning = base?.reasoning ?: variant?.reasoning ?: false,
                toolCall = base?.toolCall ?: variant?.toolCall ?: false,
                structuredOutput = base?.structuredOutput ?: variant?.structuredOutput ?: false,
                temperature = base?.temperature ?: variant?.temperature ?: false,
                openWeights = base?.openWeights ?: variant?.openWeights ?: false,
                releaseDate = base?.releaseDate ?: item?.releaseDate ?: variant?.releaseDate,
                lastUpdated = base?.lastUpdated ?: item?.updated ?: variant?.lastUpdated,
                knowledge = base?.knowledge ?: variant?.knowledge,
                modelType = base?.type ?: variant?.type,
                contextLimit = base?.limit?.context ?: item?.context ?: variant?.limit?.context,
                maxOutputTokens = base?.limit?.output ?: variant?.limit?.output,
                minInputCost = if (entries.isEmpty()) item?.inputCost else
                    entries.mapNotNull { it.model.cost?.input }.minOrNull(),
                minOutputCost = if (entries.isEmpty()) item?.outputCost else
                    entries.mapNotNull { it.model.cost?.output }.minOrNull(),
                supportsTextInput = "text" in input,
                supportsImageInput = "image" in input,
                supportsPdfInput = "pdf" in input,
                supportsAudioInput = "audio" in input,
                supportsVideoInput = "video" in input,
                supportsTextOutput = "text" in output,
                supportsImageOutput = "image" in output,
                supportsPdfOutput = "pdf" in output,
                supportsAudioOutput = "audio" in output,
                supportsVideoOutput = "video" in output,
            )
        }
        val modelsByLab = models.groupBy { it.labId }
        val labs = (labItems.keys + modelsByLab.keys).map { id ->
            val labModels = modelsByLab[id].orEmpty()
            LabEntity(
                id = id,
                name = labName(id),
                description = labItems[id]?.description,
                modelCount = labModels.size,
                providerCount = labModels.flatMap { linksByCanonicalId[it.canonicalId].orEmpty() }
                    .map { it.providerId }.distinct().size,
                lastUpdated = labModels.flatMap { listOfNotNull(it.lastUpdated, it.releaseDate) }.maxOrNull(),
            )
        }
        return CanonicalCatalogSnapshot(models, links, labs)
    }

    private data class Variant(val providerId: String, val modelId: String, val model: ModelDto)

    private companion object {
        val LAB_NAME_OVERRIDES = mapOf(
            "alibaba" to "Alibaba",
            "meta" to "Meta",
            "minimax" to "MiniMax",
            "moonshotai" to "Moonshot AI",
            "openai" to "OpenAI",
            "perplexity" to "Perplexity",
            "stepfun" to "StepFun",
            "xai" to "xAI",
            "zhipuai" to "Zhipu AI",
        )
    }
}
