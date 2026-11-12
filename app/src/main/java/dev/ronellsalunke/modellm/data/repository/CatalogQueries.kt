package dev.ronellsalunke.modellm.data.repository

import dev.ronellsalunke.modellm.data.local.dao.CanonicalDao
import dev.ronellsalunke.modellm.data.local.dao.LabDao
import dev.ronellsalunke.modellm.data.local.dao.ModelDao
import dev.ronellsalunke.modellm.data.mapper.CatalogMapper
import dev.ronellsalunke.modellm.data.mapper.toDomain
import dev.ronellsalunke.modellm.data.model.CanonicalModel
import dev.ronellsalunke.modellm.data.model.Lab
import dev.ronellsalunke.modellm.data.model.Model
import dev.ronellsalunke.modellm.data.model.ModelFilters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

open class CatalogQueries(
    private val canonicalDao: CanonicalDao,
    private val labDao: LabDao,
    private val modelDao: ModelDao,
    private val mapper: CatalogMapper,
) {
    fun observeNewestModels(limit: Int): Flow<List<CanonicalModel>> =
        canonicalDao
            .observeNewestModels(limit)
            .map { rows -> rows.map { it.toDomain() } }
            .flowOn(Dispatchers.Default)

    fun observeCanonicalModels(
        query: String = "",
        filters: ModelFilters = ModelFilters(),
    ): Flow<List<CanonicalModel>> =
        canonicalDao
            .observeModels(CatalogQueryBuilder.buildCanonicalQuery(query, filters))
            .map { rows -> rows.map { it.toDomain() } }
            .flowOn(Dispatchers.Default)

    fun observeCanonicalModel(canonicalId: String): Flow<CanonicalModel?> =
        canonicalDao.observeModel(canonicalId).map { it?.toDomain() }.flowOn(Dispatchers.Default)

    fun observeCanonicalProviders(canonicalId: String): Flow<List<Model>> =
        modelDao
            .observeCanonicalProviders(canonicalId)
            .map { rows -> rows.map(mapper::toModel) }
            .flowOn(Dispatchers.Default)

    fun observeLabs(): Flow<List<Lab>> =
        labDao
            .observeLabs()
            .map { rows -> rows.map { it.toDomain() } }
            .flowOn(Dispatchers.Default)

    fun observeLab(labId: String): Flow<Lab?> =
        labDao.observeLab(labId).map { it?.toDomain() }.flowOn(Dispatchers.Default)

    fun observeLabModels(labId: String): Flow<List<CanonicalModel>> =
        canonicalDao
            .observeLabModels(labId)
            .map { rows -> rows.map { it.toDomain() } }
            .flowOn(Dispatchers.Default)
}
