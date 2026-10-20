package dev.ronellsalunke.modellm.data.mapper

import dev.ronellsalunke.modellm.data.local.entity.CanonicalModelEntity
import dev.ronellsalunke.modellm.data.local.entity.CanonicalProviderEntity
import dev.ronellsalunke.modellm.data.local.entity.LabEntity
import dev.ronellsalunke.modellm.data.remote.CatalogPayload
import dev.ronellsalunke.modellm.data.remote.dto.ModelDto
import dev.ronellsalunke.modellm.data.remote.dto.ProviderDto
import dev.ronellsalunke.modellm.data.remote.dto.SearchIndexItemDto

data class CanonicalCatalogSnapshot(
    val models: List<CanonicalModelEntity>,
    val providers: List<CanonicalProviderEntity>,
    val labs: List<LabEntity>,
)

/** Builds canonical models, provider links, and labs from the three catalog endpoints. */
class CanonicalCatalogBuilder {
    fun build(payload: CatalogPayload): CanonicalCatalogSnapshot {
        val modelItems =
            payload.searchIndex
                .filter { it.type == "model" && !it.id.isNullOrBlank() }
                .associateBy { it.id!!.lowercase() }
        val labItems =
            payload.searchIndex
                .filter { it.type == "lab" && !it.id.isNullOrBlank() }
                .associateBy { it.id!!.lowercase() }
        val canonicalModels =
            payload.canonicalModels.entries.associateBy({ it.key.lowercase() }, { it.value })

        val canonicalRegistry = buildCanonicalRegistry(payload)
        val providers = payload.providers.filter { !it.id.isNullOrBlank() }.associateBy { it.id!! }
        val variants = collectVariants(providers, canonicalRegistry)

        val links =
            variants
                .flatMap { (id, entries) ->
                    entries.map { CanonicalProviderEntity(id, it.providerId, it.modelId) }
                }
                .distinct()
        val linksByCanonicalId = links.groupBy { it.canonicalId }

        val models =
            canonicalRegistry.values.map { id ->
                buildCanonicalModel(
                    id = id,
                    base = canonicalModels[id.lowercase()],
                    entries = variants[id].orEmpty(),
                    item = modelItems[id.lowercase()],
                    providers = providers,
                    linksByCanonicalId = linksByCanonicalId,
                    labItems = labItems,
                )
            }

        val modelsByLab = models.groupBy { it.labId }
        val labs = buildLabs(labItems, modelsByLab, linksByCanonicalId, providers)

        return CanonicalCatalogSnapshot(models, links, labs)
    }

    private fun buildCanonicalRegistry(payload: CatalogPayload): LinkedHashMap<String, String> {
        val registry = LinkedHashMap<String, String>()
        (payload.canonicalModels.keys +
                payload.searchIndex.filter { it.type == "model" }.mapNotNull { it.id })
            .filter { it.isNotBlank() }
            .forEach { registry.putIfAbsent(it.lowercase(), it) }
        return registry
    }

    private fun collectVariants(
        providers: Map<String, ProviderDto>,
        canonicalRegistry: MutableMap<String, String>,
    ): Map<String, List<Variant>> {
        val variants = mutableMapOf<String, MutableList<Variant>>()
        for ((providerId, provider) in providers) {
            for ((key, model) in provider.models.orEmpty()) {
                val resolved = extractVariant(providerId, key, model, canonicalRegistry)
                if (resolved != null) {
                    variants.getOrPut(resolved.first) { mutableListOf() }.add(resolved.second)
                }
            }
        }
        return variants
    }

    private fun extractVariant(
        providerId: String,
        key: String,
        model: ModelDto,
        canonicalRegistry: MutableMap<String, String>,
    ): Pair<String, Variant>? {
        if (model.status == "alpha") return null
        val modelId = model.id?.takeIf { it.isNotBlank() } ?: key
        val declaredId = model.canonicalModelId?.takeIf { it.isNotBlank() }
        val canonicalId =
            if (modelId.isNotBlank()) {
                resolveCanonicalId(declaredId, canonicalRegistry)
                    ?: resolveCanonicalId(key, canonicalRegistry)
                    ?: resolveCanonicalId("$providerId/$key", canonicalRegistry)
                    ?: declaredId?.let { registerCanonicalId(it, canonicalRegistry) }
            } else {
                null
            }
        return canonicalId?.let { it to Variant(providerId, modelId, model) }
    }

    private fun resolveCanonicalId(id: String?, registry: Map<String, String>): String? =
        id?.takeIf { it.isNotBlank() }?.let { registry[it.lowercase()] }

    private fun registerCanonicalId(id: String, registry: MutableMap<String, String>): String =
        registry.getOrPut(id.lowercase()) { id }

    private fun buildCanonicalModel(
        id: String,
        base: ModelDto?,
        entries: List<Variant>,
        item: SearchIndexItemDto?,
        providers: Map<String, ProviderDto>,
        linksByCanonicalId: Map<String, List<CanonicalProviderEntity>>,
        labItems: Map<String, SearchIndexItemDto>,
    ): CanonicalModelEntity {
        val variant = entries.firstOrNull()?.model
        val labId = resolveLabId(id, item, entries)
        val input = (base?.modalities?.input ?: variant?.modalities?.input).orEmpty()
        val output = (base?.modalities?.output ?: variant?.modalities?.output).orEmpty()
        val (minInputCost, minOutputCost) = resolveCosts(entries, item)
        val providerCount =
            if (entries.isNotEmpty()) linksByCanonicalId[id].orEmpty().size
            else item?.providerCount?.coerceAtLeast(0) ?: 0
        val providerNames =
            entries
                .map { providers[it.providerId]?.name ?: it.providerId }
                .distinct()
                .joinToString(" ")

        return CanonicalModelEntity(
            canonicalId = id,
            name = listOfNotNull(base?.name, item?.title, variant?.name, id).first(),
            description =
                listOfNotNull(base?.description, item?.description, variant?.description)
                    .firstOrNull(),
            family = base?.family ?: variant?.family,
            labId = labId,
            labName = resolveLabName(labId, labItems, providers),
            providerNames = providerNames,
            providerCount = providerCount,
            attachment = listOfNotNull(base?.attachment, variant?.attachment, false).first(),
            reasoning = listOfNotNull(base?.reasoning, variant?.reasoning, false).first(),
            toolCall = listOfNotNull(base?.toolCall, variant?.toolCall, false).first(),
            structuredOutput =
                listOfNotNull(base?.structuredOutput, variant?.structuredOutput, false).first(),
            temperature = listOfNotNull(base?.temperature, variant?.temperature, false).first(),
            openWeights = listOfNotNull(base?.openWeights, variant?.openWeights, false).first(),
            releaseDate =
                listOfNotNull(base?.releaseDate, item?.releaseDate, variant?.releaseDate)
                    .firstOrNull(),
            lastUpdated =
                listOfNotNull(base?.lastUpdated, item?.updated, variant?.lastUpdated)
                    .firstOrNull(),
            knowledge = base?.knowledge ?: variant?.knowledge,
            modelType = base?.type ?: variant?.type,
            contextLimit =
                listOfNotNull(base?.limit?.context, item?.context, variant?.limit?.context)
                    .firstOrNull(),
            maxOutputTokens = base?.limit?.output ?: variant?.limit?.output,
            minInputCost = minInputCost,
            minOutputCost = minOutputCost,
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

    private fun resolveLabId(
        id: String,
        item: SearchIndexItemDto?,
        entries: List<Variant>,
    ): String {
        val prefix = id.substringBefore('/', "").takeIf { id.contains('/') && it.isNotBlank() }
        return (prefix ?: item?.lab ?: entries.firstOrNull()?.providerId ?: id).lowercase()
    }

    private fun resolveCosts(
        entries: List<Variant>,
        item: SearchIndexItemDto?,
    ): Pair<Double?, Double?> {
        val inputCost =
            if (entries.isEmpty()) item?.inputCost
            else entries.mapNotNull { it.model.cost?.input }.minOrNull()
        val outputCost =
            if (entries.isEmpty()) item?.outputCost
            else entries.mapNotNull { it.model.cost?.output }.minOrNull()
        return inputCost to outputCost
    }

    private fun buildLabs(
        labItems: Map<String, SearchIndexItemDto>,
        modelsByLab: Map<String, List<CanonicalModelEntity>>,
        linksByCanonicalId: Map<String, List<CanonicalProviderEntity>>,
        providers: Map<String, ProviderDto>,
    ): List<LabEntity> =
        (labItems.keys + modelsByLab.keys).map { id ->
            val labModels = modelsByLab[id].orEmpty()
            LabEntity(
                id = id,
                name = resolveLabName(id, labItems, providers),
                description = labItems[id]?.description,
                modelCount = labModels.size,
                providerCount =
                    labModels
                        .flatMap { linksByCanonicalId[it.canonicalId].orEmpty() }
                        .map { it.providerId }
                        .distinct()
                        .size,
                lastUpdated =
                    labModels
                        .flatMap { listOfNotNull(it.lastUpdated, it.releaseDate) }
                        .maxOrNull(),
            )
        }

    private fun resolveLabName(
        id: String,
        labItems: Map<String, SearchIndexItemDto>,
        providers: Map<String, ProviderDto>,
    ): String {
        val lower = id.lowercase()
        return labItems[lower]?.title
            ?: LAB_NAME_OVERRIDES[lower]
            ?: providers[lower]?.name
            ?: id.split('-').joinToString(" ") { it.replaceFirstChar(Char::uppercaseChar) }
    }

    private data class Variant(
        val providerId: String,
        val modelId: String,
        val model: ModelDto,
    )

    private companion object {
        val LAB_NAME_OVERRIDES =
            mapOf(
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
