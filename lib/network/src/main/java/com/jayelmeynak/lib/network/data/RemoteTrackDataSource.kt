package com.jayelmeynak.lib.network.data

import com.jayelmeynak.lib.network.data.dto.ResponseChart
import com.jayelmeynak.lib.network.data.dto.TrackDto
import com.jayelmeynak.util.result.DataError
import com.jayelmeynak.util.result.Result

public interface RemoteTrackDataSource {

    public suspend fun getTrack(id: String): Result<TrackDto, DataError.Remote>

    public suspend fun getAlbum(id: String): Result<ResponseChart, DataError.Remote>
}