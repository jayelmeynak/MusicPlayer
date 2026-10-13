package com.jayelmeynak.feature.player.impl.artwork

import android.content.ContentUris
import androidx.core.net.toUri
import com.jayelmeynak.lib.mediastore.data.isMediaStoreUri
import com.jayelmeynak.lib.mediastore.domain.usecase.GetTrackArtworkUseCase
import javax.inject.Inject

/** Обложка локального трека из кэша обложек; id — `content://` URI трека. */
internal fun interface LocalArtworkSource {
    suspend fun artwork(trackId: String): ByteArray?
}

internal class MediaStoreArtworkSource @Inject constructor(
    private val getTrackArtworkUseCase: GetTrackArtworkUseCase,
) : LocalArtworkSource {

    override suspend fun artwork(trackId: String): ByteArray? {
        if (!isMediaStoreUri(trackId)) return null
        // Ключ кэша — id строки MediaStore, последний сегмент content URI.
        val rowId = runCatching { ContentUris.parseId(trackId.toUri()) }.getOrNull()?.takeIf { it >= 0 }
            ?: return null
        return getTrackArtworkUseCase(rowId, trackId)
    }
}
