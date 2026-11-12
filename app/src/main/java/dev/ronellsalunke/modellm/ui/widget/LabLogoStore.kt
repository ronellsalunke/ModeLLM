package dev.ronellsalunke.modellm.ui.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.view.WindowManager
import androidx.core.graphics.drawable.toBitmap
import coil3.SingletonImageLoader
import coil3.asDrawable
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class LabLogoStore(private val context: Context) {
    private val sizePx =
        (WidgetLogoSize.value * context.resources.displayMetrics.density).roundToInt()
    // One lock per logo so distinct logos load in parallel while a repeated request for the
    // same logo waits for the first download instead of writing the cache twice.
    private val locks = ConcurrentHashMap<String, Mutex>()

    suspend fun loadAll(labIds: Collection<String>): Map<String, Bitmap> = coroutineScope {
        val labs = labIds.distinct()
        val gate = Semaphore(MAX_PARALLEL_LOADS)
        labs.map { lab ->
            async {
                gate.withPermit {
                    load(lab, labs.size)?.let { lab to it }
                }
            }
        }.awaitAll().filterNotNull().toMap()
    }

    private suspend fun load(labId: String, distinctImageCount: Int): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                val budget = bitmapBudget(distinctImageCount)
                var targetSize = sizePx.coerceAtLeast(1)
                while (
                    targetSize.toLong() * targetSize * BytesPerPixel > budget &&
                        targetSize > 1
                ) {
                    targetSize = (targetSize / 2).coerceAtLeast(1)
                }
                val key = "$labId-$targetSize"
                locks.getOrPut(key) { Mutex() }.withLock {
                    loadLogo(labId, targetSize, budget)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
        }

    private suspend fun loadLogo(labId: String, targetSize: Int, budget: Long): Bitmap? {
        val safeLabId = labId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val directory = File(context.cacheDir, "widget_logos").apply { mkdirs() }
        val file = directory.resolve("$safeLabId-$targetSize.png")
        decodeCached(file, targetSize, budget)?.let { return it }
        val request = ImageRequest.Builder(context)
            .data("https://models.dev/logos/labs/$labId.svg")
            .size(targetSize, targetSize)
            .allowHardware(false)
            .build()
        val result = SingletonImageLoader.get(context).execute(request) as? SuccessResult
        return result?.image?.asDrawable(context.resources)?.let { drawable ->
            val bitmap = drawable.toBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
            writeCache(file, bitmap)
            bitmap
        }
    }

    // Write through a temporary file so a concurrent reader never opens a partial PNG.
    private fun writeCache(file: File, bitmap: Bitmap) {
        val temporary = File(file.parentFile, "${file.name}.tmp")
        FileOutputStream(temporary).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, PngQuality, out)
        }
        if (!temporary.renameTo(file)) {
            temporary.copyTo(file, overwrite = true)
            temporary.delete()
        }
    }

    private fun decodeCached(file: File, sizePx: Int, budget: Long): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (
            bounds.outWidth / sample > sizePx * 2 ||
                bounds.outHeight / sample > sizePx * 2 ||
                (bounds.outWidth / sample).coerceAtLeast(1).toLong() *
                (bounds.outHeight / sample).coerceAtLeast(1) * BytesPerPixel > budget
        ) {
            sample *= 2
        }
        return BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        })
    }

    // RemoteViews shares a display-sized bitmap cap across all distinct lab logos.
    // Keep each logo at its row size as window metrics can exceed the boot-time cap.
    private fun bitmapBudget(imageCount: Int): Long {
        val bounds = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.getSystemService(WindowManager::class.java)?.currentWindowMetrics?.bounds
        } else {
            null
        }
        val metrics = context.resources.displayMetrics
        val width = bounds?.width()?.takeIf { it > 0 } ?: metrics.widthPixels
        val height = bounds?.height()?.takeIf { it > 0 } ?: metrics.heightPixels
        return (CapBytesPerPixel * width * height / imageCount.coerceAtLeast(1))
            .coerceAtLeast(BytesPerPixel)
    }

    private companion object {
        const val MAX_PARALLEL_LOADS = 4
        const val BytesPerPixel = 4L
        const val CapBytesPerPixel = 6L
        const val PngQuality = 100
    }
}
