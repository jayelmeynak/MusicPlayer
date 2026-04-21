package com.jayelmeynak.local.domain.repository

import android.net.Uri
import com.jayelmeynak.local.domain.model.LocalTrack

internal interface LocalTracksRepository {
    suspend fun getTracksList(): List<LocalTrack>

    suspend fun getArtwork(trackId: Long, uri: Uri): ByteArray?

    suspend fun pruneArtworkCache(activeTrackIds: List<Long>)
}
