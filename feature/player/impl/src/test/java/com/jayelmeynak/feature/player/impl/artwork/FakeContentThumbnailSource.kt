package com.jayelmeynak.feature.player.impl.artwork

import android.graphics.Bitmap
import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import java.io.FileNotFoundException

/** Эскизы по URI; нет записи — как у провайдера без эскиза, `FileNotFoundException`. */
internal class FakeContentThumbnailSource(
    var thumbnails: Map<String, Bitmap> = emptyMap(),
) : ContentThumbnailSource {

    /** Запросы этих URI ждут, пока тест не завершит deferred. */
    val pending = mutableMapOf<String, CompletableDeferred<Bitmap>>()

    /** Запросы: URI и запрошенный предел стороны. */
    val requested = mutableListOf<Pair<String, Int>>()

    /** URI, загрузка которых отменена во время ожидания. */
    val cancelled = mutableListOf<String>()

    override suspend fun thumbnail(uri: Uri, maxSizePx: Int): Bitmap {
        val key = uri.toString()
        requested += key to maxSizePx
        val waiting = pending[key] ?: return thumbnails[key] ?: throw FileNotFoundException(key)
        try {
            return waiting.await()
        } catch (e: CancellationException) {
            cancelled += key
            throw e
        }
    }
}
