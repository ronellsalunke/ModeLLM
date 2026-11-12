package dev.ronellsalunke.modellm.ui.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import dev.ronellsalunke.modellm.MainActivity
import dev.ronellsalunke.modellm.R
import dev.ronellsalunke.modellm.data.model.CanonicalModel
import dev.ronellsalunke.modellm.data.repository.CatalogRepository
import dev.ronellsalunke.modellm.data.settings.AppSettings
import dev.ronellsalunke.modellm.data.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class NewestModelsWidget : GlanceAppWidget(), KoinComponent {
    override val sizeMode = SizeMode.Exact

    private val settingsRepository: SettingsRepository by inject()
    private val repository: CatalogRepository by inject()
    private val logos: LabLogoStore by inject()
    private val refresher: WidgetRefresher by inject()

    override suspend fun provideGlance(context: Context, id: GlanceId) = coroutineScope {
        // Compose the rows first with letter fallbacks, then swap in the logos. Waiting for
        // every remote logo before the first frame would leave the loading layout on screen.
        val initial = withContext(Dispatchers.IO) {
            WidgetSnapshot(
                settings = settingsRepository.settings.first(),
                models = repository.observeNewestModels(WidgetModelLimit).first(),
                logos = emptyMap(),
                logosLoaded = false,
                revision = refresher.revisions.value,
            )
        }
        val content = mutableStateOf(initial)
        // updateAll can reuse an active Glance session. Observe changes outside composition
        // so that session also receives fresh data and bitmaps without per-size decoding.
        launch(Dispatchers.IO) {
            combine(
                settingsRepository.settings,
                repository.observeNewestModels(WidgetModelLimit),
                refresher.revisions,
            ) { settings, models, revision -> Triple(settings, models, revision) }
                .distinctUntilChanged()
                .collect { (settings, models, revision) ->
                    val previous = content.value
                    if (!previous.logosLoaded ||
                        previous.differsFrom(settings, models, revision)
                    ) {
                        content.value = snapshot(settings, models, revision)
                    }
                }
        }
        provideContent {
            val captured = content.value
            GlanceTheme(colors = widgetColors(captured.settings)) {
                NewestModelsContent(captured.models, captured.logos)
            }
        }
    }

    private suspend fun snapshot(
        settings: AppSettings,
        models: List<CanonicalModel>,
        revision: Long,
    ): WidgetSnapshot {
        val bitmaps = logos.loadAll(models.map { it.labId })
        return WidgetSnapshot(settings, models, bitmaps, logosLoaded = true, revision = revision)
    }

    override fun onCompositionError(
        context: Context,
        glanceId: GlanceId,
        appWidgetId: Int,
        throwable: Throwable,
    ) {
        val fallback = RemoteViews(context.packageName, android.R.layout.simple_list_item_1).apply {
            setTextViewText(android.R.id.text1, context.getString(R.string.widget_empty_body))
        }
        AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, fallback)
    }
}

class NewestModelsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NewestModelsWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetThemeWorker.schedule(context, force = true)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WidgetThemeWorker.cancel(context)
    }
}

private data class WidgetSnapshot(
    val settings: AppSettings,
    val models: List<CanonicalModel>,
    val logos: Map<String, Bitmap>,
    val logosLoaded: Boolean,
    val revision: Long,
) {
    fun differsFrom(
        settings: AppSettings,
        models: List<CanonicalModel>,
        revision: Long,
    ): Boolean = settings != this.settings || models != this.models || revision != this.revision
}

@Composable
private fun NewestModelsContent(models: List<CanonicalModel>, logos: Map<String, Bitmap>) {
    Column(
        modifier = GlanceModifier.fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground),
    ) {
        WidgetHeader()
        Box(
            modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
            contentAlignment = Alignment.Center,
        ) {
            if (models.isEmpty()) {
                EmptyWidgetContent()
            } else {
                LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                    items(models, itemId = { it.id.hashCode().toLong() }) { model ->
                        ModelRow(model, logos[model.labId])
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetHeader() {
    val context = LocalContext.current
    Row(
        modifier = GlanceModifier.fillMaxWidth().height(48.dp).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = context.getString(R.string.widget_header),
            modifier = GlanceModifier.defaultWeight(),
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            ),
            maxLines = 1,
        )
        Box(
            modifier = GlanceModifier.size(48.dp)
                .clickable(actionRunCallback<RefreshWidgetAction>()),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_widget_refresh),
                contentDescription = context.getString(R.string.widget_refresh),
                modifier = GlanceModifier.size(20.dp),
                colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurfaceVariant),
            )
        }
    }
}

@Composable
private fun EmptyWidgetContent() {
    val context = LocalContext.current
    Column(
        modifier = GlanceModifier.padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = context.getString(R.string.widget_empty_title),
            style = TextStyle(
                color = GlanceTheme.colors.onBackground,
                fontWeight = FontWeight.Medium,
            ),
        )
        Text(
            text = context.getString(R.string.widget_empty_body),
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
        )
    }
}

@Composable
private fun ModelRow(model: CanonicalModel, bitmap: Bitmap?) {
    val context = LocalContext.current
    val intent = Intent(context, MainActivity::class.java)
        .putExtra(MainActivity.EXTRA_CANONICAL_ID, model.id)
        .addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        )
    Row(
        modifier = GlanceModifier.fillMaxWidth().height(48.dp)
            .padding(horizontal = 12.dp).clickable(actionStartActivity(intent)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = GlanceModifier.size(WidgetLogoSize), contentAlignment = Alignment.Center) {
            if (bitmap != null) {
                Image(
                    provider = ImageProvider(bitmap),
                    contentDescription = model.labName,
                    modifier = GlanceModifier.fillMaxSize(),
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.onBackground),
                )
            } else {
                Text(
                    text = model.labName.take(2).uppercase(),
                    style = TextStyle(color = GlanceTheme.colors.onBackground, fontSize = 12.sp),
                    maxLines = 1,
                )
            }
        }
        Spacer(GlanceModifier.width(12.dp))
        Text(
            text = model.name,
            modifier = GlanceModifier.defaultWeight(),
            style = TextStyle(
                color = GlanceTheme.colors.onBackground,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            ),
            maxLines = 1,
        )
    }
}
