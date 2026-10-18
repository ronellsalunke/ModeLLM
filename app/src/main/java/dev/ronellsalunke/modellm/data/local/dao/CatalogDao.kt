package dev.ronellsalunke.modellm.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.ronellsalunke.modellm.data.local.entity.CatalogMetadataEntity
import dev.ronellsalunke.modellm.data.local.entity.ModelEntity
import dev.ronellsalunke.modellm.data.local.entity.ModelSearchEntity
import dev.ronellsalunke.modellm.data.local.entity.ProviderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CatalogDao {
    @Query("SELECT lastSyncedAt FROM catalog_metadata WHERE id = 0")
    fun observeLastSyncedAt(): Flow<Long?>

    @Query("SELECT lastSyncedAt FROM catalog_metadata WHERE id = 0")
    suspend fun getLastSyncedAt(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putMetadata(metadata: CatalogMetadataEntity)

    @Insert
    suspend fun insertProviders(providers: List<ProviderEntity>)

    @Insert
    suspend fun insertModels(models: List<ModelEntity>): List<Long>

    @Insert
    suspend fun insertSearchEntries(entries: List<ModelSearchEntity>)

    @Query("DELETE FROM models_fts")
    suspend fun deleteSearchEntries()

    @Query("DELETE FROM models")
    suspend fun deleteModels()

    @Query("DELETE FROM providers")
    suspend fun deleteProviders()
}
