package com.jayelmeynak.feature.player.impl.artwork

import android.content.ContentUris
import androidx.core.net.toUri
import com.jayelmeynak.feature.player.impl.resolver.isMediaStoreUri
import com.jayelmeynak.lib.mediastore.domain.usecase.GetTrackArtworkUseCase
import javax.inject.Inject

/** Cover art of a local track from the artwork cache; the id is the track's `content://` URI. */
internal fun interface LocalArtworkSource {
    suspend fun artwork(trackId: String): ByteArray?
}

internal class MediaStoreArtworkSource @Inject constructor(
    private val getTrackArtworkUseCase: GetTrackArtworkUseCase,
) : LocalArtworkSource {

    override suspend fun artwork(trackId: String): ByteArray? {
        if (!isMediaStoreUri(trackId)) return null
        val uri = trackId.toUri()
        // The cache is keyed by the MediaStore row id, the last segment of the content URI.
        val rowId = runCatching { ContentUris.parseId(uri) }.getOrNull()?.takeIf { it >= 0 }
            ?: return null
        return getTrackArtworkUseCase(rowId, uri)
    }
}
