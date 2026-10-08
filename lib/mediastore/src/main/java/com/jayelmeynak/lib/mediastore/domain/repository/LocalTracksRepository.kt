package com.jayelmeynak.lib.mediastore.domain.repository

import android.net.Uri
import com.jayelmeynak.lib.mediastore.domain.model.LocalTrack

public interface LocalTracksRepository {
    public suspend fun getTracksList(): List<LocalTrack>

    public suspend fun getArtwork(trackId: Long, uri: Uri): ByteArray?

    public suspend fun pruneArtworkCache(activeTrackIds: List<Long>)
}
