package com.jayelmeynak.feature.player.impl.resolver

import com.jayelmeynak.lib.network.data.RemoteTrackDataSource
import com.jayelmeynak.lib.network.data.dto.ResponseChart
import com.jayelmeynak.lib.network.data.dto.TrackDto
import com.jayelmeynak.util.result.DataError
import com.jayelmeynak.util.result.Result

internal class FakeRemoteTrackDataSource(
    var tracks: Map<String, Result<TrackDto, DataError.Remote>> = emptyMap(),
) : RemoteTrackDataSource {

    val requestedTracks = mutableListOf<String>()

    override suspend fun getTrack(id: String): Result<TrackDto, DataError.Remote> {
        requestedTracks += id
        return tracks[id] ?: Result.Error(DataError.Remote.NOT_FOUND)
    }

    override suspend fun getAlbum(id: String): Result<ResponseChart, DataError.Remote> =
        error("not used")
}
