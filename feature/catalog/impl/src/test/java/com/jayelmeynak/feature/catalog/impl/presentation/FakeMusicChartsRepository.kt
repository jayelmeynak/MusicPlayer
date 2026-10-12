package com.jayelmeynak.feature.catalog.impl.presentation

import com.jayelmeynak.util.result.DataError
import com.jayelmeynak.util.result.Result
import com.jayelmeynak.feature.catalog.impl.domain.model.Track
import com.jayelmeynak.feature.catalog.impl.domain.repository.MusicChartsRepository
import kotlinx.coroutines.delay

internal class FakeMusicChartsRepository(
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
