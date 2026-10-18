package dev.ronellsalunke.modellm.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import dev.ronellsalunke.modellm.data.local.entity.CanonicalModelEntity
import dev.ronellsalunke.modellm.data.local.entity.CanonicalProviderEntity
import dev.ronellsalunke.modellm.data.local.entity.CanonicalSearchEntity
import dev.ronellsalunke.modellm.data.local.entity.LabEntity
import dev.ronellsalunke.modellm.data.local.entity.ModelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CanonicalDao {
    @RawQuery(observedEntities = [
        CanonicalModelEntity::class,
        CanonicalSearchEntity::class,
        CanonicalProviderEntity::class,
        ModelEntity::class,
    ])
    fun observeModels(query: SupportSQLiteQuery): Flow<List<CanonicalModelEntity>>

    @Query("SELECT * FROM canonical_models WHERE LOWER(canonicalId) = LOWER(:canonicalId)")
    fun observeModel(canonicalId: String): Flow<CanonicalModelEntity?>

    @Query("SELECT * FROM canonical_models WHERE labId = :labId ORDER BY releaseDate DESC, name COLLATE NOCASE")
    fun observeLabModels(labId: String): Flow<List<CanonicalModelEntity>>

    @Query("SELECT * FROM labs ORDER BY name COLLATE NOCASE")
    fun observeLabs(): Flow<List<LabEntity>>

    @Query("SELECT * FROM labs WHERE id = :labId")
    fun observeLab(labId: String): Flow<LabEntity?>

    @Insert
    suspend fun insertModels(models: List<CanonicalModelEntity>): List<Long>

    @Insert
    suspend fun insertSearchEntries(entries: List<CanonicalSearchEntity>)

    @Insert
    suspend fun insertProviders(providers: List<CanonicalProviderEntity>)

    @Insert
    suspend fun insertLabs(labs: List<LabEntity>)

    @Query("DELETE FROM canonical_models_fts")
    suspend fun deleteSearchEntries()

    @Query("DELETE FROM canonical_model_providers")
    suspend fun deleteProviders()

    @Query("DELETE FROM canonical_models")
    suspend fun deleteModels()

    @Query("DELETE FROM labs")
    suspend fun deleteLabs()
}
