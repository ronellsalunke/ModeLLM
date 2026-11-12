package dev.ronellsalunke.modellm.ui.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * ACTION_CONFIGURATION_CHANGED is registration-only, not manifest-deliverable. Below API 31,
 * Glance resolves day/night colors at composition, so this hourly check is the activity-independent
 * fallback instead of a persistent service. Work exists only while widgets exist.
 */
class WidgetThemeWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params), KoinComponent {
    private val refresher: WidgetRefresher by inject()

    override suspend fun doWork(): Result {
        // Refresh unconditionally: app-start and sync refreshes can change the rendered mode
        // between scheduled runs, so a cached mode can go stale.
        refresher.tryRefresh(TAG)
        return Result.success()
    }

    companion object {
        private const val TAG = "WidgetThemeWorker"
        private const val WORK_NAME = "widget-theme-check"

        fun schedule(context: Context, force: Boolean = false) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                cancel(context)
                return
            }
            if (!force && !hasWidgets(context)) return
            val request = PeriodicWorkRequestBuilder<WidgetThemeWorker>(1, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        private fun hasWidgets(context: Context): Boolean =
            AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, NewestModelsWidgetReceiver::class.java))
                .isNotEmpty()
    }
}
