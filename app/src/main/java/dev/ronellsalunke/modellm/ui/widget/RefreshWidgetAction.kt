package dev.ronellsalunke.modellm.ui.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import dev.ronellsalunke.modellm.data.sync.CatalogSyncScheduler
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class RefreshWidgetAction : ActionCallback, KoinComponent {
    private val scheduler: CatalogSyncScheduler by inject()

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        scheduler.syncNow()
    }
}
