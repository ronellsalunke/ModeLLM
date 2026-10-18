package dev.ronellsalunke.modellm.data.repository

import androidx.room.withTransaction
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import dev.ronellsalunke.modellm.data.local.ModeLLMDatabase
import dev.ronellsalunke.modellm.data.local.dao.CatalogDao
import dev.ronellsalunke.modellm.data.local.dao.ModelDao
import dev.ronellsalunke.modellm.data.local.dao.ProviderDao
import dev.ronellsalunke.modellm.data.local.entity.CatalogMetadataEntity
import dev.ronellsalunke.modellm.data.mapper.CatalogMapper
import dev.ronellsalunke.modellm.data.model.Capability
import dev.ronellsalunke.modellm.data.model.Model
import dev.ronellsalunke.modellm.data.model.ModelFilters
import dev.ronellsalunke.modellm.data.model.ModelSort
import dev.ronellsalunke.modellm.data.model.Modality
import dev.ronellsalunke.modellm.data.model.Provider
import dev.ronellsalunke.modellm.data.remote.CatalogRemoteDataSource
import dev.ronellsalunke.modellm.data.remote.dto.ProviderDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class CatalogRepository(
    private val database: ModeLLMDatabase,
    private val remote: CatalogRemoteDataSource,
    private val mapper: CatalogMapper,
    private val providerDao: ProviderDao,
    private val modelDao: ModelDao,
    private val catalogDao: CatalogDao,
) {
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val syncMutex = Mutex()

    fun observeLastSyncedAt(): Flow<Long?> =
        catalogDao.observeLastSyncedAt().flowOn(Dispatchers.IO)

    fun observeProviders(): Flow<List<Provider>> =
        providerDao.observeProviders()
            .map { rows -> rows.map(mapper::toProvider) }
            .flowOn(Dispatchers.Default)

    fun observeProvider(providerId: String): Flow<Provider?> =
        providerDao.observeProvider(providerId)
            .map { row -> row?.let(mapper::toProvider) }
            .flowOn(Dispatchers.Default)

    fun observeModels(
        query: String = "",
        filters: ModelFilters = ModelFilters(),
        sort: ModelSort = ModelSort.Newest,
    ): Flow<List<Model>> =
        modelDao.observeModels(buildModelQuery(query, filters, sort))
            .map { rows -> rows.map(mapper::toModel) }
            .flowOn(Dispatchers.Default)

    fun observeModel(providerId: String, modelId: String): Flow<Model?> =
        modelDao.observeModel(providerId, modelId)
            .map { row -> row?.let(mapper::toModel) }
            .flowOn(Dispatchers.Default)

    fun observeModelsByProvider(providerId: String): Flow<List<Model>> =
        modelDao.observeModelsByProvider(providerId)
            .map { rows -> rows.map(mapper::toModel) }
            .flowOn(Dispatchers.Default)

    /**
     * Downloads the full catalogue at most once every 15 minutes and replaces the
     * local snapshot in one transaction.
     * A failed download leaves the existing data untouched.
     */
    suspend fun refresh() {
        syncMutex.withLock {
            val lastSyncedAt = catalogDao.getLastSyncedAt()
            if (lastSyncedAt != null &&
                System.currentTimeMillis() - lastSyncedAt < MIN_REFRESH_INTERVAL_MILLIS
            ) {
                return@withLock
            }

            _syncState.value = SyncState.Running
            try {
                val syncedAt = replaceCatalog(remote.fetchCatalog())
                _syncState.value = SyncState.Success(syncedAt)
            } catch (e: CancellationException) {
                _syncState.value = SyncState.Idle
                throw e
            } catch (e: Exception) {
                _syncState.value = SyncState.Failed(e.message ?: e.javaClass.simpleName)
                throw e
            }
        }
    }

    private suspend fun replaceCatalog(providers: List<ProviderDto>): Long =
        withContext(Dispatchers.IO) {
            val providerEntities = providers.mapNotNull(mapper::toProviderEntity).distinctBy { it.id }
            val providerById = providerEntities.associateBy { it.id }
            val modelRows = providers.flatMap { dto ->
                val provider = dto.id?.let(providerById::get) ?: return@flatMap emptyList()
                dto.models.orEmpty().map { (modelKey, modelDto) ->
                    provider to mapper.toModelEntity(provider.id, modelKey, modelDto)
                }
            }.filter { (_, model) -> model.modelId.isNotBlank() }

            check(providerEntities.isNotEmpty() && modelRows.isNotEmpty()) {
                "The catalogue response contained no usable providers or models"
            }

            val syncedAt = System.currentTimeMillis()
            database.withTransaction {
                catalogDao.deleteSearchEntries()
                catalogDao.deleteModels()
                catalogDao.deleteProviders()
                catalogDao.insertProviders(providerEntities)

                modelRows.chunked(INSERT_CHUNK_SIZE).forEach { chunk ->
                    val rowIds = catalogDao.insertModels(chunk.map { (_, model) -> model })
                    catalogDao.insertSearchEntries(
                        chunk.mapIndexed { index, (provider, model) ->
                            mapper.toSearchEntity(rowIds[index], model, provider.name)
                        },
                    )
                }

                catalogDao.putMetadata(CatalogMetadataEntity(lastSyncedAt = syncedAt))
            }
            syncedAt
        }

    private fun buildModelQuery(
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

        val whereClause = conditions
            .takeIf { it.isNotEmpty() }
            ?.joinToString(prefix = "WHERE ", separator = " AND ")
            .orEmpty()

        val sql = """
            SELECT m.*, p.name AS providerName
            FROM models m
            JOIN providers p ON p.id = m.providerId
            $whereClause
            ORDER BY ${sort.orderBy()}
        """.trimIndent()

        return SimpleSQLiteQuery(sql, args.toTypedArray())
    }

    /**
     * Converts free text into an FTS4 prefix query. Tokens are split on anything that is
     * not a letter or digit, so user input can never be interpreted as FTS syntax.
     * Space-separated terms are an implicit AND in the standard FTS4 query syntax.
     */
    private fun toFtsQuery(raw: String): String? {
        val tokens = raw.lowercase()
            .split(NON_ALPHANUMERIC)
            .filter { it.isNotEmpty() }
        return tokens
            .takeIf { it.isNotEmpty() }
            ?.joinToString(separator = " ") { "$it*" }
    }

    private fun placeholders(count: Int): String =
        List(count) { "?" }.joinToString()

    private fun Modality.inputColumn(): String = when (this) {
        Modality.Text -> "m.supportsTextInput"
        Modality.Image -> "m.supportsImageInput"
        Modality.Pdf -> "m.supportsPdfInput"
        Modality.Audio -> "m.supportsAudioInput"
        Modality.Video -> "m.supportsVideoInput"
    }

    private fun Capability.column(): String = when (this) {
        Capability.Reasoning -> "m.reasoning"
        Capability.ToolCall -> "m.toolCall"
        Capability.StructuredOutput -> "m.structuredOutput"
        Capability.Attachment -> "m.attachment"
        Capability.Temperature -> "m.temperature"
    }

    private fun ModelSort.orderBy(): String = when (this) {
        ModelSort.Newest ->
            "m.releaseDate IS NULL ASC, m.releaseDate DESC, m.lastUpdated DESC, m.name COLLATE NOCASE ASC"
        ModelSort.Cheapest ->
            "m.costInput IS NULL ASC, m.costInput ASC, m.costOutput IS NULL ASC, m.costOutput ASC, m.name COLLATE NOCASE ASC"
        ModelSort.LargestContext ->
            "m.contextLimit IS NULL ASC, m.contextLimit DESC, m.name COLLATE NOCASE ASC"
        ModelSort.Name ->
            "m.name COLLATE NOCASE ASC"
    }

    private companion object {
        const val MIN_REFRESH_INTERVAL_MILLIS = 15 * 60 * 1000L
        const val INSERT_CHUNK_SIZE = 500
        val NON_ALPHANUMERIC = Regex("[^\\p{L}\\p{N}]+")
    }
}
