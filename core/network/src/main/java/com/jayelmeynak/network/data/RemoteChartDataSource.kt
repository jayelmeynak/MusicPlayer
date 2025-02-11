package com.jayelmeynak.network.data

import com.jayelmeynak.network.data.dto.ResponseChart
import com.jayelmeynak.network.data.dto.Tracks
import com.jayelmeynak.network.utils.DataError
import com.jayelmeynak.network.utils.Result

interface RemoteChartDataSource {

    suspend fun getChartSongs(): Result<ResponseChart, DataError.Remote>

    suspend fun searchTrack(q: String): Result<Tracks, DataError.Remote>

}