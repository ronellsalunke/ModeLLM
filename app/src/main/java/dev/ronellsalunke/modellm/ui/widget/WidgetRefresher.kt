package dev.ronellsalunke.modellm.ui.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import dev.ronellsalunke.modellm.data.repository.CatalogRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

class WidgetRefresher(
    private val context: Context,
    private val repository: CatalogRepository,
    private val logos: LabLogoStore,
) {
    private val revision = MutableStateFlow(0L)
    internal val revisions = revision.asStateFlow()

    suspend fun refresh() {
        val ids = GlanceAppWidgetManager(context).getGlanceIds(NewestModelsWidget::class.java)
        if (ids.isEmpty()) return
        withContext(Dispatchers.IO) {
            val models = repository.observeNewestModels(WidgetModelLimit).first()
            logos.loadAll(models.map { it.labId })
        }
        // An active Glance session must reload logos even if catalog rows did not change.
        revision.update { it + 1 }
        withContext(Dispatchers.Main) {
            NewestModelsWidget().updateAll(context)
        }
    }

    suspend fun tryRefresh(tag: String): Boolean {
        val result = runCatching { refresh() }
        val error = result.exceptionOrNull()
        if (error is CancellationException) throw error
        error?.let { Log.w(tag, "Widget refresh failed", it) }
        return result.isSuccess
    }
}
