package com.jayelmeynak.lib.mediastore.data.source

import android.net.Uri
import com.jayelmeynak.lib.mediastore.data.TrackDbo

internal interface LocalTracksDataSource {
    suspend fun getTracksList(): List<TrackDbo>

    suspend fun getArtwork(trackId: Long, uri: Uri): ByteArray?

    suspend fun pruneArtworkCache(activeTrackIds: List<Long>)
}
