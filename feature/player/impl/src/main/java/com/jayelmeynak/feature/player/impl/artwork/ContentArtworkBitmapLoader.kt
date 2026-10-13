package com.jayelmeynak.feature.player.impl.artwork

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSourceBitmapLoader
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.google.common.util.concurrent.SettableFuture
import com.jayelmeynak.util.coroutines.ApplicationScope
import com.jayelmeynak.util.coroutines.Dispatcher
import com.jayelmeynak.util.coroutines.MusicPlayerDispatchers
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Загрузчик обложек сессии (уведомление, системный UI). `content://`-обложка — URI, обложка которого —
 * эскиз провайдера (у локального трека — URI самого аудиофайла, его не декодирует ни один загрузчик
 * картинок): берётся [ContentThumbnailSource] не с main и только для показываемого элемента —
 * и из метаданных, и через [loadBitmap]. Остальное — [delegate].
 */
@OptIn(UnstableApi::class)
internal class ContentArtworkBitmapLoader(
    private val delegate: BitmapLoader,
    private val thumbnails: ContentThumbnailSource,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
) : BitmapLoader {

    @Inject
    constructor(
        @ApplicationContext context: Context,
        thumbnails: ContentThumbnailSource,
        @ApplicationScope scope: CoroutineScope,
        @Dispatcher(MusicPlayerDispatchers.IO) ioDispatcher: CoroutineDispatcher,
    ) : this(
        // Полноразмерная обложка уходила бы в уведомление и метаданные платформенной сессии
        // через IPC: картинки по адресу уменьшаются так же, как эскизы.
        DataSourceBitmapLoader.Builder(context).setMaximumOutputDimension(MAX_ARTWORK_SIZE_PX).build(),
        thumbnails,
        scope,
        ioDispatcher,
    )

    override fun supportsMimeType(mimeType: String): Boolean = delegate.supportsMimeType(mimeType)

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> = delegate.decodeBitmap(data)

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> =
        if (uri.scheme == ContentResolver.SCHEME_CONTENT) loadThumbnail(uri) else delegate.loadBitmap(uri)

    override fun loadBitmapFromMetadata(metadata: MediaMetadata): ListenableFuture<Bitmap>? {
        val uri = metadata.artworkUri
        if (metadata.artworkData != null || uri?.scheme != ContentResolver.SCHEME_CONTENT) {
            return delegate.loadBitmapFromMetadata(metadata)
        }
        return loadThumbnail(uri)
    }

    private fun loadThumbnail(uri: Uri): ListenableFuture<Bitmap> {
        val result = SettableFuture.create<Bitmap>()
        val job = scope.launch(ioDispatcher) {
            try {
                result.set(thumbnails.thumbnail(uri, MAX_ARTWORK_SIZE_PX).fitInto(MAX_ARTWORK_SIZE_PX))
            } catch (e: CancellationException) {
                result.cancel(false)
                throw e
            } catch (e: Exception) {
                result.setException(e)
            }
        }
        // Media3 отменяет future при смене элемента: загрузка эскиза для него больше не нужна.
        result.addListener({ if (result.isCancelled) job.cancel() }, MoreExecutors.directExecutor())
        return result
    }

    /**
     * Провайдер уменьшает эскиз только целым шагом выборки и может вернуть картинку крупнее
     * запрошенной: дожимаем до [maxSizePx] по большей стороне с сохранением пропорций.
     */
    private fun Bitmap.fitInto(maxSizePx: Int): Bitmap {
        val largest = maxOf(width, height)
        if (largest <= maxSizePx) return this
        val scale = maxSizePx.toFloat() / largest
        val scaled = Bitmap.createScaledBitmap(
            this,
            (width * scale).roundToInt().coerceAtLeast(1),
            (height * scale).roundToInt().coerceAtLeast(1),
            true,
        )
        if (scaled !== this) recycle()
        return scaled
    }

    internal companion object {
        /**
         * Потолок большей стороны обложки для сессии (уведомление, системный UI): эскиз провайдера
         * запрашивается в этом размере; `DataSourceBitmapLoader` уменьшает картинку степенями двойки,
         * пока она не уложится в лимит (3000 px → 750 px).
         */
        const val MAX_ARTWORK_SIZE_PX: Int = 1_024
    }
}
