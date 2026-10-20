package dev.ronellsalunke.modellm.di

import androidx.room.Room
import dev.ronellsalunke.modellm.data.local.ModeLLMDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    single {
        Room.databaseBuilder(
                androidContext(),
                ModeLLMDatabase::class.java,
                "modellm.db",
            )
            // Pre-release schema v2. The catalog is fully re-downloadable,
            // so a schema change resets the local snapshot instead of keeping stale tables.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    single { get<ModeLLMDatabase>().providerDao() }
    single { get<ModeLLMDatabase>().modelDao() }
    single { get<ModeLLMDatabase>().catalogDao() }
    single { get<ModeLLMDatabase>().canonicalDao() }
    single { get<ModeLLMDatabase>().labDao() }
}
