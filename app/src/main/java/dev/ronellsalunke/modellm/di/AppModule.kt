package dev.ronellsalunke.modellm.di

import dev.ronellsalunke.modellm.data.repository.CatalogRepository
import dev.ronellsalunke.modellm.data.settings.SettingsRepository
import dev.ronellsalunke.modellm.data.settings.appSettingsDataStore
import dev.ronellsalunke.modellm.data.sync.CatalogSyncScheduler
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val repositoryModule = module {
    single {
        CatalogRepository(
            database = get(),
            remote = get(),
            mapper = get(),
            providerDao = get(),
            modelDao = get(),
            catalogDao = get(),
        )
    }

    single { CatalogSyncScheduler(androidContext()) }
    single { SettingsRepository(androidContext().appSettingsDataStore) }
}

val appModules = listOf(networkModule, databaseModule, repositoryModule, viewModelModule)
