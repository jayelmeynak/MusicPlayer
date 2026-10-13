package com.jayelmeynak.feature.player.impl.artwork

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.CancellationSignal
import android.os.OperationCanceledException
import android.util.Size
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import javax.inject.Inject

/**
 * Эскиз `content://` URI от его провайдера: у локального трека — встроенная обложка аудиофайла.
 * Нет эскиза — исключение (`FileNotFoundException` и т. п.). Зовётся не с main.
 */
internal fun interface ContentThumbnailSource {
    suspend fun thumbnail(uri: Uri, maxSizePx: Int): Bitmap
}

internal class ContentResolverThumbnailSource(
    private val load: (uri: Uri, size: Size, signal: CancellationSignal) -> Bitmap,
) : ContentThumbnailSource {

    @Inject
    constructor(@ApplicationContext context: Context) : this(
        { uri, size, signal -> context.contentResolver.loadThumbnail(uri, size, signal) },
    )

    override suspend fun thumbnail(uri: Uri, maxSizePx: Int): Bitmap {
        val size = Size(maxSizePx, maxSizePx)
        val signal = CancellationSignal()
        return try {
            coroutineScope {
                // Загрузка блокирует поток и прерывания не замечает: отмену корутины к провайдеру
                // доносит CancellationSignal из отдельного наблюдателя.
                val cancelOnCancellation = launch(start = CoroutineStart.UNDISPATCHED) {
                    try {
                        awaitCancellation()
                    } finally {
                        signal.cancel()
                    }
                }
                try {
                    runInterruptible { load(uri, size, signal) }
                } finally {
                    cancelOnCancellation.cancel()
                }
            }
        } catch (e: OperationCanceledException) {
            // Провайдер отвечает на отменённый сигнал своим исключением: при отменённой корутине это отмена.
            currentCoroutineContext().ensureActive()
            throw e
        }
    }
}
