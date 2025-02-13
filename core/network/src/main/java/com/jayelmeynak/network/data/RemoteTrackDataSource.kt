package com.jayelmeynak.network.data

import com.jayelmeynak.network.data.dto.ResponseChart
import com.jayelmeynak.network.data.dto.TrackDto
import com.jayelmeynak.network.utils.DataError
import com.jayelmeynak.network.utils.Result

interface RemoteTrackDataSource {

    suspend fun getTrack(id: String): Result<TrackDto, DataError.Remote>

    suspend fun getAlbum(id: String): Result<ResponseChart, DataError.Remote>
}