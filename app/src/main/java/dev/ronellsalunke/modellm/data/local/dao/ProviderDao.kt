package dev.ronellsalunke.modellm.data.local.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Query
import dev.ronellsalunke.modellm.data.local.entity.ProviderEntity
import kotlinx.coroutines.flow.Flow

data class ProviderWithCount(
    @Embedded val provider: ProviderEntity,
    val modelCount: Int,
)

@Dao
interface ProviderDao {
    @Query(
        """
        SELECT p.*, (SELECT COUNT(*) FROM models m WHERE m.providerId = p.id) AS modelCount
        FROM providers p
        ORDER BY p.name COLLATE NOCASE
        """
    )
    fun observeProviders(): Flow<List<ProviderWithCount>>

    @Query(
        """
        SELECT p.*, (SELECT COUNT(*) FROM models m WHERE m.providerId = p.id) AS modelCount
        FROM providers p
        WHERE p.id = :providerId
        """
    )
    fun observeProvider(providerId: String): Flow<ProviderWithCount?>
}
