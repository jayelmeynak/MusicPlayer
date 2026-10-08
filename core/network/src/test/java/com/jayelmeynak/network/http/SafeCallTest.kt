package com.jayelmeynak.network.http

import com.jayelmeynak.util.result.DataError
import com.jayelmeynak.util.result.Result
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class SafeCallTest {

    private interface TestApi {
        @GET("/item")
        suspend fun item(): Response<Item>
    }

    private data class Item(val name: String)

    private val server = MockWebServer()
    private lateinit var api: TestApi

    @Before
    fun setUp() {
        server.start()
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TestApi::class.java)
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `успешный ответ сервера - Success с разобранным телом`() = runTest {
        server.enqueue(MockResponse.Builder().code(200).body("""{"name":"track"}""").build())

        val result = safeCall { api.item() }

        assertEquals(Result.Success(Item("track")), result)
    }

    @Test
    fun `ответ сервера 429 - TOO_MANY_REQUESTS`() = runTest {
        server.enqueue(MockResponse.Builder().code(429).build())

        val result = safeCall { api.item() }

        assertEquals(Result.Error(DataError.Remote.TOO_MANY_REQUESTS), result)
    }

    @Test
    fun `SocketTimeoutException - REQUEST_TIMEOUT`() = runTest {
        val result = safeCall<Item> { throw SocketTimeoutException() }

        assertEquals(Result.Error(DataError.Remote.REQUEST_TIMEOUT), result)
    }

    @Test
    fun `UnknownHostException - NO_INTERNET`() = runTest {
        val result = safeCall<Item> { throw UnknownHostException() }

        assertEquals(Result.Error(DataError.Remote.NO_INTERNET), result)
    }

    @Test
    fun `прочее исключение - UNKNOWN`() = runTest {
        val result = safeCall<Item> { throw IOException() }

        assertEquals(Result.Error(DataError.Remote.UNKNOWN), result)
    }

    @Test
    fun `отменённая корутина - отмена пробрасывается, код после safeCall не выполняется`() = runTest {
        var reachedAfterCall = false
        val job = launch {
            safeCall<Item> {
                currentCoroutineContext().cancel()
                throw IOException()
            }
            reachedAfterCall = true
        }

        job.join()

        assertTrue(job.isCancelled)
        assertFalse(reachedAfterCall)
    }
}
