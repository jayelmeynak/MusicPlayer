package com.jayelmeynak.feature.catalog.impl.resolver

import com.jayelmeynak.lib.network.data.RemoteTrackDataSource
import com.jayelmeynak.lib.network.data.dto.ResponseChart
import com.jayelmeynak.lib.network.data.dto.TrackDto
import com.jayelmeynak.util.result.DataError
import com.jayelmeynak.util.result.Result
import kotlinx.coroutines.CompletableDeferred

internal class FakeRemoteTrackDataSource(
    var tracks: Map<String, Result<TrackDto, DataError.Remote>> = emptyMap(),
) : RemoteTrackDataSource {

    val requestedTracks = mutableListOf<String>()

    /** Ворота ответа по id: запрос ждёт, пока тест их не откроет. */
    var gates: Map<String, CompletableDeferred<Unit>> = emptyMap()

    override suspend fun getTrack(id: String): Result<TrackDto, DataError.Remote> {
        requestedTracks += id
        gates[id]?.await()
        return tracks[id] ?: Result.Error(DataError.Remote.NOT_FOUND)
    }

    override suspend fun getAlbum(id: String): Result<ResponseChart, DataError.Remote> =
        error("not used")
}
