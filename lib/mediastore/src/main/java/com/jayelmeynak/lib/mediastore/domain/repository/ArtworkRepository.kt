package com.jayelmeynak.lib.mediastore.domain.repository

/** Кэш встроенных обложек аудиофайлов устройства, по id строки MediaStore; [getArtwork] читает файл по его `content://` [uri]. */
public interface ArtworkRepository {
    public suspend fun getArtwork(trackId: Long, uri: String): ByteArray?

    public suspend fun pruneArtworkCache(activeTrackIds: List<Long>)
}
