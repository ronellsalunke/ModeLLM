package dev.ronellsalunke.modellm.data.repository

import dev.ronellsalunke.modellm.data.local.ModeLLMDatabase
import dev.ronellsalunke.modellm.data.local.dao.CanonicalDao
import dev.ronellsalunke.modellm.data.local.dao.CatalogDao
import dev.ronellsalunke.modellm.data.local.dao.LabDao
import dev.ronellsalunke.modellm.data.local.dao.ModelDao
import dev.ronellsalunke.modellm.data.local.dao.ProviderDao
import dev.ronellsalunke.modellm.data.mapper.CatalogMapper
import dev.ronellsalunke.modellm.data.model.Model
import dev.ronellsalunke.modellm.data.model.ModelFilters
import dev.ronellsalunke.modellm.data.model.ModelSort
import dev.ronellsalunke.modellm.data.model.Provider
import dev.ronellsalunke.modellm.data.remote.CatalogRemoteDataSource
import java.io.IOException
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
import kotlinx.serialization.SerializationException

class CatalogRepository(
    database: ModeLLMDatabase,
    private val remote: CatalogRemoteDataSource,
    private val mapper: CatalogMapper,
    private val providerDao: ProviderDao,
    private val modelDao: ModelDao,
    private val catalogDao: CatalogDao,
    private val canonicalDao: CanonicalDao = database.canonicalDao(),
    private val labDao: LabDao = database.labDao(),
) : CatalogQueries(
    canonicalDao = canonicalDao,
    labDao = labDao,
    modelDao = modelDao,
    mapper = mapper,
) {
    private val syncer =
        CatalogSyncer(
            database = database,
            catalogDao = catalogDao,
            canonicalDao = canonicalDao,
            labDao = labDao,
            mapper = mapper,
        )

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val syncMutex = Mutex()

    fun observeLastSyncedAt(): Flow<Long?> = catalogDao.observeLastSyncedAt().flowOn(Dispatchers.IO)

    fun observeProviders(): Flow<List<Provider>> =
        providerDao
            .observeProviders()
            .map { rows -> rows.map(mapper::toProvider) }
            .flowOn(Dispatchers.Default)

    fun observeProvider(providerId: String): Flow<Provider?> =
        providerDao
            .observeProvider(providerId)
            .map { row -> row?.let(mapper::toProvider) }
            .flowOn(Dispatchers.Default)

    fun observeModels(
        query: String = "",
        filters: ModelFilters = ModelFilters(),
        sort: ModelSort = ModelSort.Newest,
    ): Flow<List<Model>> =
        modelDao
            .observeModels(CatalogQueryBuilder.buildModelQuery(query, filters, sort))
            .map { rows -> rows.map(mapper::toModel) }
            .flowOn(Dispatchers.Default)

    fun observeModel(providerId: String, modelId: String): Flow<Model?> =
        modelDao
            .observeModel(providerId, modelId)
            .map { row -> row?.let { mapper.toModel(it, includeReasoningOptions = true) } }
            .flowOn(Dispatchers.Default)

    fun observeModelsByProvider(providerId: String): Flow<List<Model>> =
        modelDao
            .observeModelsByProvider(providerId)
            .map { rows -> rows.map(mapper::toModel) }
            .flowOn(Dispatchers.Default)

    /**
     * Downloads the full catalogue and replaces the local snapshot in one transaction. Automatic
     * syncs download only when the catalogue is empty or at least 24 hours old. Forced syncs
     * ([force] = true) always download, with no cooldown. A failed download leaves the existing
     * data untouched.
     */
    suspend fun refresh(force: Boolean = false) {
        syncMutex.withLock {
            try {
                if (!force && !syncer.isAutomaticRefreshDue()) {
                    return@withLock
                }

                _syncState.value = SyncState.Running
                val syncedAt = syncer.replaceCatalog(remote.fetchCatalog())
                _syncState.value = SyncState.Success(syncedAt)
            } catch (e: CancellationException) {
                _syncState.value = SyncState.Idle
                throw e
            } catch (e: IOException) {
                handleSyncFailure(e)
            } catch (e: SerializationException) {
                handleSyncFailure(e)
            } catch (e: IllegalStateException) {
                handleSyncFailure(e)
            } catch (e: IllegalArgumentException) {
                handleSyncFailure(e)
            } catch (e: android.database.SQLException) {
                handleSyncFailure(e)
            }
        }
    }

    private fun handleSyncFailure(cause: Throwable): Nothing {
        val message = cause.message ?: cause.javaClass.simpleName
        _syncState.value = SyncState.Failed(message)
        throw CatalogSyncException(message, cause)
    }
}
