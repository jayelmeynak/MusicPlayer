package com.jayelmeynak.download_tracks.presentation

import android.net.Uri
import com.jayelmeynak.lib.mediastore.domain.model.LocalTrack
import com.jayelmeynak.lib.mediastore.domain.repository.LocalTracksRepository
import kotlinx.coroutines.CompletableDeferred

class FakeLocalTracksRepository(
    var tracks: List<LocalTrack> = emptyList(),
    var artworks: Map<Long, ByteArray?> = emptyMap(),
) : LocalTracksRepository {

    val prunedIds = mutableListOf<List<Long>>()
    var tracksRequests = 0

    /** Thrown by getTracksList, e.g. a SecurityException without the audio permission. */
    var tracksError: Exception? = null

    /** Artwork requests for these ids throw. */
    var failingArtworks: Set<Long> = emptySet()

    /** Artwork requests for these ids suspend until the test completes the deferred. */
    val pendingArtworks = mutableMapOf<Long, CompletableDeferred<ByteArray?>>()

    override suspend fun getTracksList(): List<LocalTrack> {
        tracksRequests++
        tracksError?.let { throw it }
        return tracks
    }

    override suspend fun getArtwork(trackId: Long, uri: Uri): ByteArray? {
        if (trackId in failingArtworks) throw IllegalStateException("broken artwork $trackId")
        return pendingArtworks[trackId]?.await() ?: artworks[trackId]
    }

    override suspend fun pruneArtworkCache(activeTrackIds: List<Long>) {
        prunedIds += activeTrackIds
    }
}
