package dev.ronellsalunke.modellm.data.repository

import androidx.room.withTransaction
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import dev.ronellsalunke.modellm.data.local.ModeLLMDatabase
import dev.ronellsalunke.modellm.data.local.dao.CatalogDao
import dev.ronellsalunke.modellm.data.local.dao.ModelDao
import dev.ronellsalunke.modellm.data.local.dao.ProviderDao
import dev.ronellsalunke.modellm.data.local.entity.CatalogMetadataEntity
import dev.ronellsalunke.modellm.data.mapper.CanonicalCatalogBuilder
import dev.ronellsalunke.modellm.data.mapper.CatalogMapper
import dev.ronellsalunke.modellm.data.mapper.toDomain
import dev.ronellsalunke.modellm.data.mapper.toSearchEntity
import dev.ronellsalunke.modellm.data.model.CanonicalModel
import dev.ronellsalunke.modellm.data.model.Capability
import dev.ronellsalunke.modellm.data.model.Lab
import dev.ronellsalunke.modellm.data.model.Model
import dev.ronellsalunke.modellm.data.model.ModelFilters
import dev.ronellsalunke.modellm.data.model.ModelSort
import dev.ronellsalunke.modellm.data.model.Modality
import dev.ronellsalunke.modellm.data.model.Provider
import dev.ronellsalunke.modellm.data.remote.CatalogPayload
import dev.ronellsalunke.modellm.data.remote.CatalogRemoteDataSource
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
    private val canonicalDao = database.canonicalDao()

    fun observeCanonicalModels(
        query: String = "",
        filters: ModelFilters = ModelFilters(),
        sort: ModelSort = ModelSort.Newest,
    ): Flow<List<CanonicalModel>> =
        canonicalDao.observeModels(buildCanonicalQuery(query, filters, sort))
            .map { rows -> rows.map { it.toDomain() } }
            .flowOn(Dispatchers.Default)

    fun observeCanonicalModel(canonicalId: String): Flow<CanonicalModel?> =
        canonicalDao.observeModel(canonicalId)
            .map { it?.toDomain() }
            .flowOn(Dispatchers.Default)

    fun observeCanonicalProviders(canonicalId: String): Flow<List<Model>> =
        modelDao.observeCanonicalProviders(canonicalId)
            .map { rows -> rows.map(mapper::toModel) }
            .flowOn(Dispatchers.Default)

    fun observeLabs(): Flow<List<Lab>> =
        canonicalDao.observeLabs()
            .map { rows -> rows.map { it.toDomain() } }
            .flowOn(Dispatchers.Default)

    fun observeLab(labId: String): Flow<Lab?> =
        canonicalDao.observeLab(labId)
            .map { it?.toDomain() }
            .flowOn(Dispatchers.Default)

    fun observeLabModels(labId: String): Flow<List<CanonicalModel>> =
        canonicalDao.observeLabModels(labId)
            .map { rows -> rows.map { it.toDomain() } }
            .flowOn(Dispatchers.Default)

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
            .map { row -> row?.let { mapper.toModel(it, includeReasoningOptions = true) } }
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

    private suspend fun replaceCatalog(payload: CatalogPayload): Long =
        withContext(Dispatchers.IO) {
            val providers = payload.providers
            val providerEntities = providers.mapNotNull(mapper::toProviderEntity).distinctBy { it.id }
            val providerById = providerEntities.associateBy { it.id }
            val modelRows = providers.flatMap { dto ->
                val provider = dto.id?.let(providerById::get) ?: return@flatMap emptyList()
                dto.models.orEmpty().map { (modelKey, modelDto) ->
                    provider to mapper.toModelEntity(provider.id, modelKey, modelDto)
                }
            }.filter { (_, model) -> model.modelId.isNotBlank() }

            val canonical = withContext(Dispatchers.Default) {
                CanonicalCatalogBuilder().build(payload)
            }
            val searchModels = payload.searchIndex.count { it.type == "model" && !it.id.isNullOrBlank() }
            val searchLabs = payload.searchIndex.count { it.type == "lab" && !it.id.isNullOrBlank() }
            check(
                providerEntities.isNotEmpty() && modelRows.isNotEmpty() &&
                    payload.canonicalModels.size >= 500 && searchModels >= 500 && searchLabs >= 50 &&
                    canonical.models.size >= 500 && canonical.labs.size >= 50,
            ) {
                "The catalogue response contained too few providers, models, canonical models, search models, or labs"
            }

            val syncedAt = System.currentTimeMillis()
            database.withTransaction {
                canonicalDao.deleteSearchEntries()
                canonicalDao.deleteProviders()
                canonicalDao.deleteModels()
                canonicalDao.deleteLabs()
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

                canonical.models.chunked(INSERT_CHUNK_SIZE).forEach { chunk ->
                    val rowIds = canonicalDao.insertModels(chunk)
                    canonicalDao.insertSearchEntries(
                        chunk.mapIndexed { index, model -> model.toSearchEntity(rowIds[index]) },
                    )
                }
                canonical.providers.chunked(INSERT_CHUNK_SIZE).forEach { canonicalDao.insertProviders(it) }
                canonical.labs.chunked(INSERT_CHUNK_SIZE).forEach { canonicalDao.insertLabs(it) }

                catalogDao.putMetadata(CatalogMetadataEntity(lastSyncedAt = syncedAt))
            }
            syncedAt
        }

    private fun buildCanonicalQuery(
        query: String,
        filters: ModelFilters,
        sort: ModelSort,
    ): SupportSQLiteQuery {
        val conditions = mutableListOf<String>()
        val args = mutableListOf<Any?>()

        toFtsQuery(query)?.let {
            conditions += "c.rowId IN (SELECT rowid FROM canonical_models_fts WHERE canonical_models_fts MATCH ?)"
            args += it
        }
        if (filters.labIds.isNotEmpty()) {
            conditions += "c.labId IN (${placeholders(filters.labIds.size)})"
            args.addAll(filters.labIds)
        }
        if (filters.providerIds.isNotEmpty()) {
            conditions += "c.canonicalId IN (SELECT canonicalId FROM canonical_model_providers WHERE providerId IN (${placeholders(filters.providerIds.size)}))"
            args.addAll(filters.providerIds)
        }
        filters.inputModalities.forEach { conditions += "${it.inputColumn("c")} = 1" }
        filters.capabilities.forEach { conditions += "${it.column("c")} = 1" }
        filters.openWeights?.let {
            conditions += "c.openWeights = ?"
            args += if (it) 1 else 0
        }
        if (filters.statuses.isNotEmpty()) {
            conditions += "EXISTS (SELECT 1 FROM canonical_model_providers cp " +
                "JOIN models m ON m.providerId = cp.providerId AND m.modelId = cp.providerModelId " +
                "WHERE cp.canonicalId = c.canonicalId AND m.status IN (${placeholders(filters.statuses.size)}))"
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
        val where = conditions.takeIf { it.isNotEmpty() }
            ?.joinToString(prefix = "WHERE ", separator = " AND ").orEmpty()
        val order = when (sort) {
            ModelSort.Newest ->
                "c.releaseDate IS NULL ASC, c.releaseDate DESC, c.lastUpdated DESC, c.name COLLATE NOCASE ASC"
            ModelSort.Cheapest ->
                "c.minInputCost IS NULL ASC, c.minInputCost ASC, c.minOutputCost IS NULL ASC, c.minOutputCost ASC, c.name COLLATE NOCASE ASC"
            ModelSort.LargestContext ->
                "c.contextLimit IS NULL ASC, c.contextLimit DESC, c.name COLLATE NOCASE ASC"
            ModelSort.Name -> "c.name COLLATE NOCASE ASC"
        }
        return SimpleSQLiteQuery("SELECT c.* FROM canonical_models c $where ORDER BY $order", args.toTypedArray())
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

    private fun Modality.inputColumn(alias: String = "m"): String = "$alias." + when (this) {
        Modality.Text -> "supportsTextInput"
        Modality.Image -> "supportsImageInput"
        Modality.Pdf -> "supportsPdfInput"
        Modality.Audio -> "supportsAudioInput"
        Modality.Video -> "supportsVideoInput"
    }

    private fun Capability.column(alias: String = "m"): String = "$alias." + when (this) {
        Capability.Reasoning -> "reasoning"
        Capability.ToolCall -> "toolCall"
        Capability.StructuredOutput -> "structuredOutput"
        Capability.Attachment -> "attachment"
        Capability.Temperature -> "temperature"
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
