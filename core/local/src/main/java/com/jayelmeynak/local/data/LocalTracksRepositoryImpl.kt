package com.jayelmeynak.local.data

import android.net.Uri
import com.jayelmeynak.local.data.source.LocalTracksDataSource
import com.jayelmeynak.local.domain.model.LocalTrack
import com.jayelmeynak.local.domain.repository.LocalTracksRepository
import javax.inject.Inject

internal class LocalTracksRepositoryImpl @Inject constructor(
    private val dataSource: LocalTracksDataSource
) : LocalTracksRepository {

    override suspend fun getTracksList(): List<LocalTrack> =
        dataSource.getTracksList().map { it.toLocalTrack() }

    override suspend fun getArtwork(uri: Uri): ByteArray? {
        return dataSource.getArtwork(uri)
    }
}

private fun TrackDbo.toLocalTrack() = LocalTrack(
    id = id,
    title = title,
    artistName = artist,
    duration = duration,
    uri = uri,
)
