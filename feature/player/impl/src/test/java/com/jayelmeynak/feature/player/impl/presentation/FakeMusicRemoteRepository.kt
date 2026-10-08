package com.jayelmeynak.feature.player.impl.presentation

import com.jayelmeynak.feature.player.impl.domain.models.Track
import com.jayelmeynak.feature.player.impl.domain.repository.MusicRemoteRepository
import com.jayelmeynak.util.result.DataError
import com.jayelmeynak.util.result.Result

internal class FakeMusicRemoteRepository(
    var tracks: Map<String, Track> = emptyMap(),
    var albums: Map<String, List<Track>> = emptyMap(),
) : MusicRemoteRepository {

    override suspend fun getTrack(id: String): Result<Track, DataError.Remote> =
        tracks[id]?.let { Result.Success(it) } ?: Result.Error(DataError.Remote.NOT_FOUND)

    override suspend fun getAlbum(id: String): Result<List<Track>, DataError.Remote> =
        Result.Success(albums[id].orEmpty())
}
