package com.jayelmeynak.lib.network.data

import com.jayelmeynak.lib.network.data.dto.ResponseChart
import com.jayelmeynak.lib.network.data.dto.Tracks
import com.jayelmeynak.util.result.DataError
import com.jayelmeynak.util.result.Result
import com.jayelmeynak.lib.network.http.safeCall
import javax.inject.Inject

internal class RemoteChartDataSourceImpl @Inject constructor(
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