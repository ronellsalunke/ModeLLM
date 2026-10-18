package dev.ronellsalunke.modellm.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import dev.ronellsalunke.modellm.data.local.dao.CatalogDao
import dev.ronellsalunke.modellm.data.local.dao.ModelDao
import dev.ronellsalunke.modellm.data.local.dao.ProviderDao
import dev.ronellsalunke.modellm.data.local.entity.CatalogMetadataEntity
import dev.ronellsalunke.modellm.data.local.entity.ModelEntity
import dev.ronellsalunke.modellm.data.local.entity.ModelSearchEntity
import dev.ronellsalunke.modellm.data.local.entity.ProviderEntity

@Database(
    entities = [
        ProviderEntity::class,
        ModelEntity::class,
        ModelSearchEntity::class,
        CatalogMetadataEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class ModeLLMDatabase : RoomDatabase() {
    abstract fun providerDao(): ProviderDao
    abstract fun modelDao(): ModelDao
    abstract fun catalogDao(): CatalogDao
}
