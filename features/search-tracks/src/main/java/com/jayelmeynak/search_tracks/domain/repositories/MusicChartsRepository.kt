package com.jayelmeynak.search_tracks.domain.repositories

import com.jayelmeynak.network.utils.DataError
import com.jayelmeynak.network.utils.Result
import com.jayelmeynak.search_tracks.domain.models.Track

interface MusicChartsRepository {

    suspend fun getChart(): Result<List<Track>, DataError.Remote>

    suspend fun searchTrack(q: String): Result<List<Track>, DataError.Remote>
}
