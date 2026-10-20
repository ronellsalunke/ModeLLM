package dev.ronellsalunke.modellm.data.repository

import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import dev.ronellsalunke.modellm.data.model.Capability
import dev.ronellsalunke.modellm.data.model.Modality
import dev.ronellsalunke.modellm.data.model.ModelFilters
import dev.ronellsalunke.modellm.data.model.ModelSort

internal object CatalogQueryBuilder {
    private val NON_ALPHANUMERIC = Regex("[^\\p{L}\\p{N}]+")

    fun buildCanonicalQuery(
        query: String,
        filters: ModelFilters,
        sort: ModelSort,
    ): SupportSQLiteQuery {
        val conditions = mutableListOf<String>()
        val args = mutableListOf<Any?>()

        toFtsQuery(query)?.let {
            conditions +=
                "c.rowId IN (SELECT rowid FROM canonical_models_fts " +
                    "WHERE canonical_models_fts MATCH ?)"
            args += it
        }
        appendCanonicalFilterConditions(filters, conditions, args)

        val where =
            conditions
                .takeIf { it.isNotEmpty() }
                ?.joinToString(prefix = "WHERE ", separator = " AND ")
                .orEmpty()
        val order = canonicalOrderBy(sort)
        return SimpleSQLiteQuery(
            "SELECT c.* FROM canonical_models c $where ORDER BY $order",
            args.toTypedArray(),
        )
    }

    private fun appendCanonicalFilterConditions(
        filters: ModelFilters,
        conditions: MutableList<String>,
        args: MutableList<Any?>,
    ) {
        if (filters.labIds.isNotEmpty()) {
            conditions += "c.labId IN (${placeholders(filters.labIds.size)})"
            args.addAll(filters.labIds)
        }
        if (filters.providerIds.isNotEmpty()) {
            conditions +=
                "c.canonicalId IN (SELECT canonicalId FROM canonical_model_providers " +
                    "WHERE providerId IN (${placeholders(filters.providerIds.size)}))"
            args.addAll(filters.providerIds)
        }
        filters.inputModalities.forEach { conditions += "${it.inputColumn("c")} = 1" }
        filters.capabilities.forEach { conditions += "${it.column("c")} = 1" }
        filters.openWeights?.let {
            conditions += "c.openWeights = ?"
            args += if (it) 1 else 0
        }
        if (filters.statuses.isNotEmpty()) {
            conditions +=
                "EXISTS (SELECT 1 FROM canonical_model_providers cp " +
                    "JOIN models m ON m.providerId = cp.providerId " +
                    "AND m.modelId = cp.providerModelId " +
                    "WHERE cp.canonicalId = c.canonicalId " +
                    "AND m.status IN (${placeholders(filters.statuses.size)}))"
            args.addAll(filters.statuses)
        }
        filters.knowledgeAfter?.let {
            conditions += "c.knowledge >= ?"
            args += it
        }
        filters.knowledgeBefore?.let {
            conditions += "c.knowledge <= ?"
            args += it
        }
    }

    private fun canonicalOrderBy(sort: ModelSort): String =
        when (sort) {
            ModelSort.Newest ->
                "c.releaseDate IS NULL ASC, c.releaseDate DESC, " +
                    "c.lastUpdated DESC, c.name COLLATE NOCASE ASC"
            ModelSort.Cheapest ->
                "c.minInputCost IS NULL ASC, c.minInputCost ASC, " +
                    "c.minOutputCost IS NULL ASC, c.minOutputCost ASC, c.name COLLATE NOCASE ASC"
            ModelSort.LargestContext ->
                "c.contextLimit IS NULL ASC, c.contextLimit DESC, c.name COLLATE NOCASE ASC"
            ModelSort.Name -> "c.name COLLATE NOCASE ASC"
        }

    fun buildModelQuery(
        query: String,
        filters: ModelFilters,
        sort: ModelSort,
    ): SupportSQLiteQuery {
        val conditions = mutableListOf<String>()
        val args = mutableListOf<Any?>()

        toFtsQuery(query)?.let { ftsQuery ->
            conditions += "m.rowId IN (SELECT rowid FROM models_fts WHERE models_fts MATCH ?)"
            args += ftsQuery
        }
        appendModelFilterConditions(filters, conditions, args)

        val whereClause =
            conditions
                .takeIf { it.isNotEmpty() }
                ?.joinToString(prefix = "WHERE ", separator = " AND ")
                .orEmpty()

        val sql =
            """
            SELECT m.*, p.name AS providerName
            FROM models m
            JOIN providers p ON p.id = m.providerId
            $whereClause
            ORDER BY ${modelOrderBy(sort)}
        """
                .trimIndent()

        return SimpleSQLiteQuery(sql, args.toTypedArray())
    }

    private fun appendModelFilterConditions(
        filters: ModelFilters,
        conditions: MutableList<String>,
        args: MutableList<Any?>,
    ) {
        if (filters.providerIds.isNotEmpty()) {
            conditions += "m.providerId IN (${placeholders(filters.providerIds.size)})"
            args.addAll(filters.providerIds)
        }
        filters.inputModalities.forEach { modality ->
            conditions += "${modality.inputColumn()} = 1"
        }
        filters.capabilities.forEach { capability ->
            conditions += "${capability.column()} = 1"
        }
        filters.openWeights?.let { openWeights ->
            conditions += "m.openWeights = ?"
            args += if (openWeights) 1 else 0
        }
        if (filters.statuses.isNotEmpty()) {
            conditions += "m.status IN (${placeholders(filters.statuses.size)})"
            args.addAll(filters.statuses)
        }
        filters.knowledgeAfter?.let { knowledgeAfter ->
            conditions += "m.knowledge >= ?"
            args += knowledgeAfter
        }
        filters.knowledgeBefore?.let { knowledgeBefore ->
            conditions += "m.knowledge <= ?"
            args += knowledgeBefore
        }
    }

    private fun modelOrderBy(sort: ModelSort): String =
        when (sort) {
            ModelSort.Newest ->
                "m.releaseDate IS NULL ASC, m.releaseDate DESC, " +
                    "m.lastUpdated DESC, m.name COLLATE NOCASE ASC"
            ModelSort.Cheapest ->
                "m.costInput IS NULL ASC, m.costInput ASC, " +
                    "m.costOutput IS NULL ASC, m.costOutput ASC, m.name COLLATE NOCASE ASC"
            ModelSort.LargestContext ->
                "m.contextLimit IS NULL ASC, m.contextLimit DESC, m.name COLLATE NOCASE ASC"
            ModelSort.Name -> "m.name COLLATE NOCASE ASC"
        }

    private fun toFtsQuery(raw: String): String? {
        val tokens = raw.lowercase().split(NON_ALPHANUMERIC).filter { it.isNotEmpty() }
        return tokens.takeIf { it.isNotEmpty() }?.joinToString(separator = " ") { "$it*" }
    }

    private fun placeholders(count: Int): String = List(count) { "?" }.joinToString()

    private fun Modality.inputColumn(alias: String = "m"): String =
        "$alias." +
            when (this) {
                Modality.Text -> "supportsTextInput"
                Modality.Image -> "supportsImageInput"
                Modality.Pdf -> "supportsPdfInput"
                Modality.Audio -> "supportsAudioInput"
                Modality.Video -> "supportsVideoInput"
            }

    private fun Capability.column(alias: String = "m"): String =
        "$alias." +
            when (this) {
                Capability.Reasoning -> "reasoning"
                Capability.ToolCall -> "toolCall"
                Capability.StructuredOutput -> "structuredOutput"
                Capability.Attachment -> "attachment"
                Capability.Temperature -> "temperature"
            }
}
