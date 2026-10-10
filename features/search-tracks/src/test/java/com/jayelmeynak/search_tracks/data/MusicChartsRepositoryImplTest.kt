package com.jayelmeynak.search_tracks.data

import com.jayelmeynak.lib.network.data.RemoteChartDataSource
import com.jayelmeynak.lib.network.data.dto.ResponseChart
import com.jayelmeynak.lib.network.data.dto.Tracks
import com.jayelmeynak.util.result.DataError
import com.jayelmeynak.util.result.Result
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class MusicChartsRepositoryImplTest {

    private val source = FakeRemoteChartDataSource()
    private val repository = MusicChartsRepositoryImpl(source)

    private val mixedTracks = Tracks(
        tracks = listOf(
            trackDto().copy(id = 1L),
            trackDto().copy(id = null),
            null,
            trackDto().copy(id = 2L),
            trackDto().copy(preview = null),
        )
    )

    @Test
    fun `чарт отбрасывает битые записи и сохраняет порядок`() = runTest {
        source.chart = Result.Success(ResponseChart(mixedTracks))

        val ids = (repository.getChart() as Result.Success).data.map { it.id }

        assertEquals(listOf(1L, 2L), ids)
    }

    @Test
    fun `поиск отбрасывает битые записи и сохраняет порядок`() = runTest {
        source.search = Result.Success(mixedTracks)

        val ids = (repository.searchTrack("abc") as Result.Success).data.map { it.id }

        assertEquals(listOf(1L, 2L), ids)
    }

    @Test
    fun `чарт без tracks или data - пустой список`() = runTest {
        source.chart = Result.Success(ResponseChart())
        assertEquals(Result.Success(emptyList<Any>()), repository.getChart())

        source.chart = Result.Success(ResponseChart(Tracks()))
        assertEquals(Result.Success(emptyList<Any>()), repository.getChart())
    }

    @Test
    fun `поиск без data - пустой список`() = runTest {
        source.search = Result.Success(Tracks())

        assertEquals(Result.Success(emptyList<Any>()), repository.searchTrack("abc"))
    }

    private val duplicatedTracks = Tracks(
        tracks = listOf(
            trackDto().copy(id = 1L, title = "First"),
            trackDto().copy(id = 2L),
            trackDto().copy(id = 1L, title = "Duplicate"),
            trackDto().copy(id = 3L),
        )
    )

    @Test
    fun `чарт с повтором id - трек один раз, первое вхождение, порядок сохранён`() = runTest {
        source.chart = Result.Success(ResponseChart(duplicatedTracks))

        val tracks = (repository.getChart() as Result.Success).data

        assertEquals(listOf(1L, 2L, 3L), tracks.map { it.id })
        assertEquals("First", tracks.first().title)
    }

    @Test
    fun `поиск с повтором id - трек один раз, первое вхождение, порядок сохранён`() = runTest {
        source.search = Result.Success(duplicatedTracks)

        val tracks = (repository.searchTrack("abc") as Result.Success).data

        assertEquals(listOf(1L, 2L, 3L), tracks.map { it.id })
        assertEquals("First", tracks.first().title)
    }

    @Test
    fun `ошибка источника проходит как есть`() = runTest {
        source.chart = Result.Error(DataError.Remote.NOT_FOUND)
        source.search = Result.Error(DataError.Remote.TOO_MANY_REQUESTS)

        assertEquals(Result.Error(DataError.Remote.NOT_FOUND), repository.getChart())
        assertEquals(Result.Error(DataError.Remote.TOO_MANY_REQUESTS), repository.searchTrack("abc"))
    }

    private class FakeRemoteChartDataSource : RemoteChartDataSource {
        var chart: Result<ResponseChart, DataError.Remote> = Result.Success(ResponseChart())
        var search: Result<Tracks, DataError.Remote> = Result.Success(Tracks())

        override suspend fun getChartSongs(): Result<ResponseChart, DataError.Remote> = chart

        override suspend fun searchTrack(q: String): Result<Tracks, DataError.Remote> = search
    }
}
