package com.jayelmeynak.player.presentation

import android.net.Uri
import com.jayelmeynak.lib.mediastore.domain.model.LocalTrack
import com.jayelmeynak.lib.mediastore.domain.repository.LocalTracksRepository
import kotlinx.coroutines.CompletableDeferred

class FakeLocalTracksRepository(
    var tracks: List<LocalTrack> = emptyList(),
    var artworks: Map<Long, ByteArray?> = emptyMap(),
) : LocalTracksRepository {

    /** Artwork requests for these track ids suspend until the test completes the deferred. */
    val pendingArtworks = mutableMapOf<Long, CompletableDeferred<ByteArray?>>()

    override suspend fun getTracksList(): List<LocalTrack> = tracks

    override suspend fun getArtwork(trackId: Long, uri: Uri): ByteArray? =
        pendingArtworks[trackId]?.await() ?: artworks[trackId]

    override suspend fun pruneArtworkCache(activeTrackIds: List<Long>) = Unit
}
