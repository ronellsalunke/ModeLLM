package dev.ronellsalunke.modellm.data.repository

import androidx.room.withTransaction
import dev.ronellsalunke.modellm.data.local.ModeLLMDatabase
import dev.ronellsalunke.modellm.data.local.dao.CanonicalDao
import dev.ronellsalunke.modellm.data.local.dao.CatalogDao
import dev.ronellsalunke.modellm.data.local.dao.LabDao
import dev.ronellsalunke.modellm.data.local.entity.CatalogMetadataEntity
import dev.ronellsalunke.modellm.data.local.entity.ModelEntity
import dev.ronellsalunke.modellm.data.local.entity.ProviderEntity
import dev.ronellsalunke.modellm.data.mapper.CanonicalCatalogBuilder
import dev.ronellsalunke.modellm.data.mapper.CanonicalCatalogSnapshot
import dev.ronellsalunke.modellm.data.mapper.CatalogMapper
import dev.ronellsalunke.modellm.data.mapper.toSearchEntity
import dev.ronellsalunke.modellm.data.remote.CatalogPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class CatalogSyncer(
    private val database: ModeLLMDatabase,
    private val catalogDao: CatalogDao,
    private val canonicalDao: CanonicalDao,
    private val labDao: LabDao,
    private val mapper: CatalogMapper,
) {
    suspend fun isAutomaticRefreshDue(): Boolean {
        val lastSyncedAt = catalogDao.getLastSyncedAt() ?: return true
        return System.currentTimeMillis() - lastSyncedAt >= AUTO_REFRESH_INTERVAL_MILLIS
    }

    suspend fun replaceCatalog(payload: CatalogPayload): Long =
        withContext(Dispatchers.IO) {
            val (providerEntities, modelRows) = prepareModelRows(payload)
            val canonical =
                withContext(Dispatchers.Default) {
                    CanonicalCatalogBuilder().build(payload)
                }
            validatePayload(payload, providerEntities, modelRows, canonical)

            val syncedAt = System.currentTimeMillis()
            writeCatalogToDatabase(providerEntities, modelRows, canonical, syncedAt)
            syncedAt
        }

    private fun prepareModelRows(
        payload: CatalogPayload,
    ): Pair<List<ProviderEntity>, List<Pair<ProviderEntity, ModelEntity>>> {
        val providers = payload.providers
        val providerEntities =
            providers.mapNotNull(mapper::toProviderEntity).distinctBy { it.id }
        val providerById = providerEntities.associateBy { it.id }
        val modelRows =
            providers
                .flatMap { dto ->
                    val provider = dto.id?.let(providerById::get) ?: return@flatMap emptyList()
                    dto.models.orEmpty().map { (modelKey, modelDto) ->
                        provider to mapper.toModelEntity(provider.id, modelKey, modelDto)
                    }
                }
                .filter { (_, model) -> model.modelId.isNotBlank() }
        return providerEntities to modelRows
    }

    private fun validatePayload(
        payload: CatalogPayload,
        providerEntities: List<ProviderEntity>,
        modelRows: List<Pair<ProviderEntity, ModelEntity>>,
        canonical: CanonicalCatalogSnapshot,
    ) {
        val searchModels =
            payload.searchIndex.count { it.type == "model" && !it.id.isNullOrBlank() }
        val searchLabs =
            payload.searchIndex.count { it.type == "lab" && !it.id.isNullOrBlank() }
        check(
            providerEntities.isNotEmpty() &&
                modelRows.isNotEmpty() &&
                payload.canonicalModels.size >= MIN_CANONICAL_MODELS &&
                searchModels >= MIN_SEARCH_MODELS &&
                searchLabs >= MIN_SEARCH_LABS &&
                canonical.models.size >= MIN_BUILT_CANONICAL_MODELS &&
                canonical.labs.size >= MIN_BUILT_LABS,
        ) {
            "The catalogue response contained too few providers, models, " +
                "canonical models, search models, or labs"
        }
    }

    private suspend fun writeCatalogToDatabase(
        providerEntities: List<ProviderEntity>,
        modelRows: List<Pair<ProviderEntity, ModelEntity>>,
        canonical: CanonicalCatalogSnapshot,
        syncedAt: Long,
    ) {
        database.withTransaction {
            canonicalDao.deleteSearchEntries()
            canonicalDao.deleteProviders()
            canonicalDao.deleteModels()
            labDao.deleteLabs()
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
            canonical.providers.chunked(INSERT_CHUNK_SIZE).forEach {
                canonicalDao.insertProviders(it)
            }
            canonical.labs.chunked(INSERT_CHUNK_SIZE).forEach { labDao.insertLabs(it) }

            catalogDao.putMetadata(CatalogMetadataEntity(lastSyncedAt = syncedAt))
        }
    }

    companion object {
        const val AUTO_REFRESH_INTERVAL_MILLIS = 24 * 60 * 60 * 1000L
        const val INSERT_CHUNK_SIZE = 500
        const val MIN_CANONICAL_MODELS = 500
        const val MIN_SEARCH_MODELS = 500
        const val MIN_SEARCH_LABS = 50
        const val MIN_BUILT_CANONICAL_MODELS = 500
        const val MIN_BUILT_LABS = 50
    }
}
