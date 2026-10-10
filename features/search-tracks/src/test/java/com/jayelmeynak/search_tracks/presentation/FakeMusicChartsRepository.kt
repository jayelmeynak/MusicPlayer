package com.jayelmeynak.search_tracks.presentation

import com.jayelmeynak.util.result.DataError
import com.jayelmeynak.util.result.Result
import com.jayelmeynak.search_tracks.domain.models.Track
import com.jayelmeynak.search_tracks.domain.repositories.MusicChartsRepository
import kotlinx.coroutines.delay

class FakeMusicChartsRepository(
    var chartResult: Result<List<Track>, DataError.Remote> = Result.Success(emptyList()),
    var searchResult: (String) -> Result<List<Track>, DataError.Remote> = { Result.Success(emptyList()) },
    var searchDelayMs: (String) -> Long = { 0L },
) : MusicChartsRepository {

    val searchQueries = mutableListOf<String>()
    var chartRequests = 0

    /** Задержка ответа чарта по номеру запроса (с 1). */
    var chartDelayMs: (call: Int) -> Long = { 0L }

    /** Ответ чарта по номеру запроса (с 1); без него — [chartResult]. */
    var chartResultFor: ((call: Int) -> Result<List<Track>, DataError.Remote>)? = null

    override suspend fun getChart(): Result<List<Track>, DataError.Remote> {
        val call = ++chartRequests
        delay(chartDelayMs(call))
        return chartResultFor?.invoke(call) ?: chartResult
    }

    override suspend fun searchTrack(q: String): Result<List<Track>, DataError.Remote> {
        searchQueries += q
        delay(searchDelayMs(q))
        return searchResult(q)
    }
}
