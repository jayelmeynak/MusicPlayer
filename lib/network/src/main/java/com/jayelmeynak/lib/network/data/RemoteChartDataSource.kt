package com.jayelmeynak.lib.network.data

import com.jayelmeynak.lib.network.data.dto.ResponseChart
import com.jayelmeynak.lib.network.data.dto.Tracks
import com.jayelmeynak.util.result.DataError
import com.jayelmeynak.util.result.Result

public interface RemoteChartDataSource {

    public suspend fun getChartSongs(): Result<ResponseChart, DataError.Remote>

    public suspend fun searchTrack(q: String): Result<Tracks, DataError.Remote>

}