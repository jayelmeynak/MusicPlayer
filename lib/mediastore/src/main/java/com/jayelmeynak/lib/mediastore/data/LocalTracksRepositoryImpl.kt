package com.jayelmeynak.lib.mediastore.data

import android.net.Uri
import com.jayelmeynak.lib.mediastore.data.source.LocalTracksDataSource
import com.jayelmeynak.lib.mediastore.domain.model.LocalTrack
import com.jayelmeynak.lib.mediastore.domain.repository.LocalTracksRepository
import javax.inject.Inject

internal class LocalTracksRepositoryImpl @Inject constructor(
    private val dataSource: LocalTracksDataSource
) : LocalTracksRepository {

    override suspend fun getTracksList(): List<LocalTrack> =
        dataSource.getTracksList().map { it.toLocalTrack() }

    override suspend fun getArtwork(trackId: Long, uri: Uri): ByteArray? =
        dataSource.getArtwork(trackId, uri)

    override suspend fun pruneArtworkCache(activeTrackIds: List<Long>) =
        dataSource.pruneArtworkCache(activeTrackIds)
}

private fun TrackDbo.toLocalTrack() = LocalTrack(
    id = id,
    title = title,
    artistName = artist,
    duration = duration,
    uri = uri,
)
