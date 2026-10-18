package dev.ronellsalunke.modellm.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import dev.ronellsalunke.modellm.data.local.dao.CanonicalDao
import dev.ronellsalunke.modellm.data.local.dao.CatalogDao
import dev.ronellsalunke.modellm.data.local.dao.ModelDao
import dev.ronellsalunke.modellm.data.local.dao.ProviderDao
import dev.ronellsalunke.modellm.data.local.entity.CanonicalModelEntity
import dev.ronellsalunke.modellm.data.local.entity.CanonicalProviderEntity
import dev.ronellsalunke.modellm.data.local.entity.CanonicalSearchEntity
import dev.ronellsalunke.modellm.data.local.entity.CatalogMetadataEntity
import dev.ronellsalunke.modellm.data.local.entity.LabEntity
import dev.ronellsalunke.modellm.data.local.entity.ModelEntity
import dev.ronellsalunke.modellm.data.local.entity.ModelSearchEntity
import dev.ronellsalunke.modellm.data.local.entity.ProviderEntity

@Database(
    entities = [
        ProviderEntity::class,
        ModelEntity::class,
        ModelSearchEntity::class,
        CatalogMetadataEntity::class,
        CanonicalModelEntity::class,
        CanonicalSearchEntity::class,
        CanonicalProviderEntity::class,
        LabEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class ModeLLMDatabase : RoomDatabase() {
    abstract fun providerDao(): ProviderDao
    abstract fun modelDao(): ModelDao
    abstract fun catalogDao(): CatalogDao
    abstract fun canonicalDao(): CanonicalDao
}
