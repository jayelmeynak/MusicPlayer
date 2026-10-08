package com.jayelmeynak.download_tracks.presentation

import android.net.Uri
import com.jayelmeynak.lib.mediastore.domain.model.LocalTrack
import com.jayelmeynak.lib.mediastore.domain.repository.LocalTracksRepository

class FakeLocalTracksRepository(
    var tracks: List<LocalTrack> = emptyList(),
    var artworks: Map<Long, ByteArray?> = emptyMap(),
) : LocalTracksRepository {

    val prunedIds = mutableListOf<List<Long>>()

    override suspend fun getTracksList(): List<LocalTrack> = tracks

    override suspend fun getArtwork(trackId: Long, uri: Uri): ByteArray? = artworks[trackId]

    override suspend fun pruneArtworkCache(activeTrackIds: List<Long>) {
        prunedIds += activeTrackIds
    }
}
