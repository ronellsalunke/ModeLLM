package dev.ronellsalunke.modellm.data.local.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import dev.ronellsalunke.modellm.data.local.entity.ModelEntity
import dev.ronellsalunke.modellm.data.local.entity.ModelSearchEntity
import dev.ronellsalunke.modellm.data.local.entity.ProviderEntity
import kotlinx.coroutines.flow.Flow

data class ModelWithProvider(
    @Embedded val model: ModelEntity,
    val providerName: String,
)

@Dao
interface ModelDao {
    @RawQuery(
        observedEntities = [
            ModelEntity::class,
            ProviderEntity::class,
            ModelSearchEntity::class,
        ],
    )
    fun observeModels(query: SupportSQLiteQuery): Flow<List<ModelWithProvider>>

    @Query(
        """
        SELECT m.*, p.name AS providerName
        FROM models m
        JOIN providers p ON p.id = m.providerId
        WHERE m.providerId = :providerId AND LOWER(m.modelId) = LOWER(:modelId)
        LIMIT 1
        """,
    )
    fun observeModel(providerId: String, modelId: String): Flow<ModelWithProvider?>

    @Query(
        """
        SELECT m.*, p.name AS providerName
        FROM models m
        JOIN providers p ON p.id = m.providerId
        WHERE m.providerId = :providerId
        ORDER BY m.name COLLATE NOCASE
        """,
    )
    fun observeModelsByProvider(providerId: String): Flow<List<ModelWithProvider>>

    @Query(
        """
        SELECT m.*, p.name AS providerName
        FROM models m
        JOIN providers p ON p.id = m.providerId
        JOIN canonical_model_providers c ON c.providerId = m.providerId AND c.providerModelId = m.modelId
        WHERE LOWER(c.canonicalId) = LOWER(:canonicalId)
        ORDER BY p.name COLLATE NOCASE, m.name COLLATE NOCASE
        """,
    )
    fun observeCanonicalProviders(canonicalId: String): Flow<List<ModelWithProvider>>
}
