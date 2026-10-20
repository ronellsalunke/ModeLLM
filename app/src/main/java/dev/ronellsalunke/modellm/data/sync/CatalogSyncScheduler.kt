package dev.ronellsalunke.modellm.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

class CatalogSyncScheduler(private val context: Context) {

    /** Schedules daily automatic syncs, gated to at most once per 24 hours. */
    fun schedulePeriodicSync() {
        val request =
            PeriodicWorkRequestBuilder<CatalogSyncWorker>(1, TimeUnit.DAYS)
                .setConstraints(networkConstraints())
                .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
    }

    /** Catches up on app start when the catalogue is empty or at least 24 hours old. */
    fun ensureInitialSync() {
        val request =
            OneTimeWorkRequestBuilder<CatalogSyncWorker>()
                .setConstraints(networkConstraints())
                .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                INITIAL_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request,
            )
    }

    /** Forces a download with no cooldown, replacing any pending manual request. */
    fun syncNow() {
        val request =
            OneTimeWorkRequestBuilder<CatalogSyncWorker>()
                .setInputData(workDataOf(CatalogSyncWorker.KEY_FORCE_REFRESH to true))
                .setConstraints(networkConstraints())
                .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                MANUAL_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request,
            )
    }

    private fun networkConstraints(): Constraints =
        Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    private companion object {
        const val PERIODIC_WORK_NAME = "catalog-sync-periodic"
        const val INITIAL_WORK_NAME = "catalog-sync-initial"
        const val MANUAL_WORK_NAME = "catalog-sync-manual"
    }
}
