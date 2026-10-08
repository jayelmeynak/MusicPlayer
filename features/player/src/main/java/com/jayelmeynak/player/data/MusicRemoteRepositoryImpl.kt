package com.jayelmeynak.player.data

import com.jayelmeynak.network.data.RemoteTrackDataSource
import com.jayelmeynak.util.result.DataError
import com.jayelmeynak.util.result.Result
import com.jayelmeynak.util.result.map
import com.jayelmeynak.player.domain.models.Track
import com.jayelmeynak.player.domain.repository.MusicRemoteRepository
import javax.inject.Inject

internal class MusicRemoteRepositoryImpl @Inject constructor(
    private val remoteTrackDataSource: RemoteTrackDataSource
) : MusicRemoteRepository {
    override suspend fun getTrack(id: String): Result<Track, DataError.Remote> {
        return remoteTrackDataSource
            .getTrack(id)
            .map { it.toTrack() }
    }

    override suspend fun getAlbum(id: String): Result<List<Track>, DataError.Remote> {
        return remoteTrackDataSource
            .getAlbum(id)
            .map {
                it.tracks.tracks.map { trackDto -> trackDto.toTrack() }
            }
    }
}