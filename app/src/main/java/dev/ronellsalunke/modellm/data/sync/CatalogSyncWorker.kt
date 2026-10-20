package dev.ronellsalunke.modellm.data.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.ronellsalunke.modellm.data.repository.CatalogRepository
import dev.ronellsalunke.modellm.data.repository.CatalogSyncException
import kotlinx.coroutines.CancellationException
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class CatalogSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params), KoinComponent {

    private val repository: CatalogRepository by inject()

    override suspend fun doWork(): Result {
        val force = inputData.getBoolean(KEY_FORCE_REFRESH, false)
        return try {
            repository.refresh(force = force)
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: CatalogSyncException) {
            Log.w(TAG, "Catalog sync failed", e)
            if (!force && runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val KEY_FORCE_REFRESH = "force_refresh"
        private const val TAG = "CatalogSyncWorker"
        private const val MAX_RETRIES = 3
    }
}
