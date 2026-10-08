package com.jayelmeynak.lib.network.http

import com.jayelmeynak.lib.network.data.RemoteChartDataSourceImpl
import com.jayelmeynak.lib.network.data.RemoteTrackDataSourceImpl
import com.jayelmeynak.lib.network.di.createApiService
import com.jayelmeynak.util.result.DataError
import com.jayelmeynak.util.result.Result
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeezerErrorResponseTest {

    private val server = MockWebServer()
    private lateinit var chartSource: RemoteChartDataSourceImpl
    private lateinit var trackSource: RemoteTrackDataSourceImpl

    @Before
    fun setUp() {
        server.start()
        val api = createApiService(server.url("/"), OkHttpClient())
        chartSource = RemoteChartDataSourceImpl(api)
        trackSource = RemoteTrackDataSourceImpl(api)
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `код 4 - квота - TOO_MANY_REQUESTS`() = runTest {
        enqueueDeezerError(4)

        assertEquals(Result.Error(DataError.Remote.TOO_MANY_REQUESTS), chartSource.getChartSongs())
    }

    @Test
    fun `код 700 - сервис занят - SERVER`() = runTest {
        enqueueDeezerError(700)

        assertEquals(Result.Error(DataError.Remote.SERVER), chartSource.searchTrack("abc"))
    }

    @Test
    fun `код 800 - нет данных - NOT_FOUND`() = runTest {
        enqueueDeezerError(800)

        assertEquals(Result.Error(DataError.Remote.NOT_FOUND), trackSource.getTrack("0"))
    }

    @Test
    fun `код 800 на альбоме - NOT_FOUND`() = runTest {
        enqueueDeezerError(800)

        assertEquals(Result.Error(DataError.Remote.NOT_FOUND), trackSource.getAlbum("0"))
    }

    @Test
    fun `коды лимита, прав, токена, параметров, запроса и неизвестный - UNKNOWN`() = runTest {
        for (code in listOf(100, 200, 300, 500, 501, 600, 999)) {
            enqueueDeezerError(code)

            assertEquals("code $code", Result.Error(DataError.Remote.UNKNOWN), chartSource.getChartSongs())
        }
    }

    @Test
    fun `ошибка без кода - UNKNOWN`() = runTest {
        enqueue("""{"error":{"type":"Exception","message":"oops"}}""")

        assertEquals(Result.Error(DataError.Remote.UNKNOWN), chartSource.getChartSongs())
    }

    @Test
    fun `код строкой - UNKNOWN`() = runTest {
        enqueue("""{"error":{"type":"QuotaException","message":"Quota limit exceeded","code":"4"}}""")

        assertEquals(Result.Error(DataError.Remote.UNKNOWN), chartSource.getChartSongs())
    }

    @Test
    fun `поле error внутри трека, а не на верхнем уровне - Success`() = runTest {
        enqueue("""{"data":[{"id":1,"title":"t","preview":"p","error":{"code":800}}]}""")

        val result = chartSource.searchTrack("abc")

        assertTrue(result is Result.Success)
        assertEquals(1L, (result as Result.Success).data.tracks?.single()?.id)
    }

    @Test
    fun `поиск без совпадений - Success с пустым списком`() = runTest {
        enqueue("""{"data":[],"total":0}""")

        val result = chartSource.searchTrack("zzqxq")

        assertEquals(emptyList<Any>(), (result as Result.Success).data.tracks)
    }

    @Test
    fun `корректный чарт - Success с данными`() = runTest {
        enqueue("""{"tracks":{"data":[{"id":42,"title":"Title","preview":"https://preview"}]}}""")

        val result = chartSource.getChartSongs()

        val track = (result as Result.Success).data.tracks?.tracks?.single()
        assertEquals(42L, track?.id)
        assertEquals("Title", track?.title)
        assertEquals("https://preview", track?.preview)
    }

    @Test
    fun `битый JSON - UNKNOWN, как и раньше`() = runTest {
        enqueue("""{"tracks":""")

        assertEquals(Result.Error(DataError.Remote.UNKNOWN), chartSource.getChartSongs())
    }

    @Test
    fun `HTTP 429 с телом ошибки Deezer маппится по коду HTTP`() = runTest {
        server.enqueue(
            MockResponse.Builder()
                .code(429)
                .body("""{"error":{"type":"DataException","message":"no data","code":800}}""")
                .build()
        )

        assertEquals(Result.Error(DataError.Remote.TOO_MANY_REQUESTS), chartSource.getChartSongs())
    }

    private fun enqueueDeezerError(code: Int) {
        enqueue("""{"error":{"type":"Exception","message":"message","code":$code}}""")
    }

    private fun enqueue(body: String) {
        server.enqueue(MockResponse.Builder().code(200).body(body).build())
    }
}
