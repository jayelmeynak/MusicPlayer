package com.jayelmeynak.network.data

import com.jayelmeynak.network.data.dto.ResponseChart
import javax.inject.Inject
import com.jayelmeynak.network.utils.Result
import com.jayelmeynak.network.data.dto.TrackDto
import com.jayelmeynak.network.utils.DataError
import com.jayelmeynak.network.utils.safeCall

class RemoteTrackDataSourceImpl @Inject constructor(
    private val api: ApiService
) : RemoteTrackDataSource {
    override suspend fun getTrack(id: String): Result<TrackDto, DataError.Remote> {
        return safeCall {
            api.getTrack(id)
        }
    }

    override suspend fun getAlbum(id: String): Result<ResponseChart, DataError.Remote> {
        return safeCall {
            api.getAlbum(id)
        }
    }
}