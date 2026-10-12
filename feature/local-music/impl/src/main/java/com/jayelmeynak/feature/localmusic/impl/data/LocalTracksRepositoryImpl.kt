package com.jayelmeynak.feature.localmusic.impl.data

import com.jayelmeynak.feature.localmusic.impl.data.source.LocalTracksDataSource
import com.jayelmeynak.feature.localmusic.impl.domain.model.LocalTrack
import com.jayelmeynak.feature.localmusic.impl.domain.repository.LocalTracksRepository
import com.jayelmeynak.lib.mediastore.data.TrackDbo
import javax.inject.Inject

internal class LocalTracksRepositoryImpl @Inject constructor(
    private val dataSource: LocalTracksDataSource
) : LocalTracksRepository {

    override suspend fun getTracksList(): List<LocalTrack> =
        dataSource.getTracksList().map { it.toLocalTrack() }
}

private fun TrackDbo.toLocalTrack() = LocalTrack(
    id = id,
    title = title,
    artistName = artist,
    duration = duration,
    uri = uri,
)
