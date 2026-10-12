package com.jayelmeynak.feature.catalog.impl.domain.repository

import com.jayelmeynak.feature.catalog.impl.domain.model.Track
import com.jayelmeynak.util.result.DataError
import com.jayelmeynak.util.result.Result

internal interface MusicChartsRepository {

    suspend fun getChart(): Result<List<Track>, DataError.Remote>

    suspend fun searchTrack(q: String): Result<List<Track>, DataError.Remote>
}
