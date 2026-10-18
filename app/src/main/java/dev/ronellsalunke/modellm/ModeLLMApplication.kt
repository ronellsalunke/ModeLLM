package dev.ronellsalunke.modellm

import android.app.Application
import dev.ronellsalunke.modellm.data.sync.CatalogSyncScheduler
import dev.ronellsalunke.modellm.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin

class ModeLLMApplication : Application(), KoinComponent {

    private val syncScheduler: CatalogSyncScheduler by inject()

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@ModeLLMApplication)
            modules(appModules)
        }

        syncScheduler.schedulePeriodicSync()
        syncScheduler.ensureInitialSync()
    }
}
