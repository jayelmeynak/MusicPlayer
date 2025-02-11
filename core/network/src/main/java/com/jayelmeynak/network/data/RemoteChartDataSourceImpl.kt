package com.jayelmeynak.network.data

import com.jayelmeynak.network.data.dto.ResponseChart
import com.jayelmeynak.network.data.dto.Tracks
import com.jayelmeynak.network.utils.DataError
import com.jayelmeynak.network.utils.Result
import com.jayelmeynak.network.utils.safeCall
import javax.inject.Inject

class RemoteChartDataSourceImpl @Inject constructor(
    private val api: ApiService
): RemoteChartDataSource {
    override suspend fun getChartSongs(): Result<ResponseChart, DataError.Remote> {
        return safeCall {
            api.getChartSongs()
        }
    }

    override suspend fun searchTrack(q: String): Result<Tracks, DataError.Remote> {
        return safeCall {
            api.searchSongs(q)
        }
    }
}