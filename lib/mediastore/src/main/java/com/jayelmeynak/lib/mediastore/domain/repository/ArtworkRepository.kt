package com.jayelmeynak.lib.mediastore.domain.repository

import android.net.Uri

/** Кэш встроенных обложек аудиофайлов устройства, по id строки MediaStore. */
public interface ArtworkRepository {
    public suspend fun getArtwork(trackId: Long, uri: Uri): ByteArray?

    public suspend fun pruneArtworkCache(activeTrackIds: List<Long>)
}
