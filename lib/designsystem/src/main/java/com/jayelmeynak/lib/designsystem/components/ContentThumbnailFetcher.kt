package com.jayelmeynak.lib.designsystem.components

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.CancellationSignal
import android.os.OperationCanceledException
import android.util.Size
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import coil.size.pxOrElse
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible

/** Обложка-эскиз провайдера: `content://` URI, картинку для которого строит сам провайдер. */
internal data class ContentThumbnail(val uri: Uri)

/** Загрузка эскиза; блокирующая, отмену получает через [CancellationSignal]. */
internal fun interface ThumbnailLoader {
    fun load(uri: Uri, size: Size, signal: CancellationSignal): Bitmap
}

/**
 * Эскиз `content://` URI через `ContentResolver.loadThumbnail` (API 29+): у аудио MediaStore —
 * встроенная обложка, которую провайдер уменьшает и кэширует сам. Размер — слот Coil; отмена
 * запроса (строка ушла с экрана) отменяет и загрузку у провайдера. Нет эскиза — исключение,
 * Coil показывает заглушку.
 */
internal class ContentThumbnailFetcher(
    private val uri: Uri,
    private val options: Options,
    private val loader: ThumbnailLoader,
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val size = Size(
            options.size.width.pxOrElse { DEFAULT_SIZE_PX },
            options.size.height.pxOrElse { DEFAULT_SIZE_PX },
        )
        val bitmap = loadCancellable { signal -> loader.load(uri, size, signal) }
        return DrawableResult(
            drawable = BitmapDrawable(options.context.resources, bitmap),
            // Эскиз меньше оригинала: Coil не подставит его из памяти в слот крупнее.
            isSampled = true,
            dataSource = DataSource.DISK,
        )
    }

    class Factory(private val loader: ThumbnailLoader? = null) : Fetcher.Factory<ContentThumbnail> {
        override fun create(data: ContentThumbnail, options: Options, imageLoader: ImageLoader): Fetcher =
            ContentThumbnailFetcher(
                uri = data.uri,
                options = options,
                loader = loader ?: ThumbnailLoader { uri, size, signal ->
                    options.context.contentResolver.loadThumbnail(uri, size, signal)
                },
            )
    }

    companion object {
        /** Сторона эскиза, когда Coil не знает размер слота. */
        const val DEFAULT_SIZE_PX: Int = 512
    }
}

/**
 * Блокирующая загрузка, отменяемая вместе с корутиной: отмену к провайдеру доносит [CancellationSignal]
 * (отдельный наблюдатель — поток занят загрузкой). Отменённая загрузка у провайдера бросает
 * [OperationCanceledException]: при отменённой корутине она становится обычной отменой, а не ошибкой.
 */
private suspend fun <T> loadCancellable(load: (CancellationSignal) -> T): T {
    val signal = CancellationSignal()
    return try {
        coroutineScope {
            val cancelOnCancellation = launch(start = CoroutineStart.UNDISPATCHED) {
                try {
                    awaitCancellation()
                } finally {
                    signal.cancel()
                }
            }
            try {
                runInterruptible { load(signal) }
            } finally {
                cancelOnCancellation.cancel()
            }
        }
    } catch (e: OperationCanceledException) {
        currentCoroutineContext().ensureActive()
        throw e
    }
}
