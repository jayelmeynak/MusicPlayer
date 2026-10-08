package com.jayelmeynak.feature.player.impl.data

import com.jayelmeynak.lib.network.data.RemoteTrackDataSource
import com.jayelmeynak.util.result.DataError
import com.jayelmeynak.util.result.Result
import com.jayelmeynak.util.result.map
import com.jayelmeynak.feature.player.impl.domain.models.Track
import com.jayelmeynak.feature.player.impl.domain.repository.MusicRemoteRepository
import javax.inject.Inject

internal class MusicRemoteRepositoryImpl @Inject constructor(
    private val remoteTrackDataSource: RemoteTrackDataSource
) : MusicRemoteRepository {
    override suspend fun getTrack(id: String): Result<Track, DataError.Remote> {
        return when (val result = remoteTrackDataSource.getTrack(id)) {
            is Result.Error -> result
            is Result.Success -> result.data.toTrack()
                ?.let { Result.Success(it) }
                ?: Result.Error(DataError.Remote.SERIALIZATION)
        }
    }

    override suspend fun getAlbum(id: String): Result<List<Track>, DataError.Remote> {
        return remoteTrackDataSource
            .getAlbum(id)
            .map {
                it.tracks?.tracks.orEmpty().mapNotNull { trackDto -> trackDto?.toTrack() }
            }
    }
}