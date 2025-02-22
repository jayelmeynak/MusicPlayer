package com.jayelmeynak.player.data

import com.jayelmeynak.local.data.LocalTracksDataSource
import com.jayelmeynak.player.domain.models.Track
import com.jayelmeynak.player.domain.repository.MusicLocalRepository
import javax.inject.Inject

class MusicLocalRepositoryImpl @Inject constructor(
    private val localTracksDataSource: LocalTracksDataSource
) : MusicLocalRepository {
    override fun getTrackByUri(uri: String): Track? {
        return localTracksDataSource.getTrack(uri)?.toTrack()
    }

    override fun getTracksList(): List<Track> {
        return localTracksDataSource.getTracksList().map {
            it.toTrack()
        }
    }
}