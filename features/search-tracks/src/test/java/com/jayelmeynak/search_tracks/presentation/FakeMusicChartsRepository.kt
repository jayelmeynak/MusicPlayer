package com.jayelmeynak.search_tracks.presentation

import com.jayelmeynak.util.result.DataError
import com.jayelmeynak.util.result.Result
import com.jayelmeynak.search_tracks.domain.models.Track
import com.jayelmeynak.search_tracks.domain.repositories.MusicChartsRepository

class FakeMusicChartsRepository(
    var chartResult: Result<List<Track>, DataError.Remote> = Result.Success(emptyList()),
    var searchResult: (String) -> Result<List<Track>, DataError.Remote> = { Result.Success(emptyList()) },
) : MusicChartsRepository {

    val searchQueries = mutableListOf<String>()

    override suspend fun getChart(): Result<List<Track>, DataError.Remote> = chartResult

    override suspend fun searchTrack(q: String): Result<List<Track>, DataError.Remote> {
        searchQueries += q
        return searchResult(q)
    }
}
