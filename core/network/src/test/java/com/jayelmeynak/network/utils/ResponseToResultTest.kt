package com.jayelmeynak.network.utils

import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Response

class ResponseToResultTest {

    @Test
    fun `успешный ответ с телом - Success с телом`() = runTest {
        val result = responseToResult(Response.success("body"))

        assertEquals(Result.Success("body"), result)
    }

    @Test
    fun `успешный ответ без тела - SERIALIZATION`() = runTest {
        val result = responseToResult(Response.success<String>(null))

        assertEquals(Result.Error(DataError.Remote.SERIALIZATION), result)
    }

    @Test
    fun `код 408 - REQUEST_TIMEOUT`() = runTest {
        assertEquals(Result.Error(DataError.Remote.REQUEST_TIMEOUT), errorResult(408))
    }

    @Test
    fun `код 429 - TOO_MANY_REQUESTS`() = runTest {
        assertEquals(Result.Error(DataError.Remote.TOO_MANY_REQUESTS), errorResult(429))
    }

    @Test
    fun `коды 5xx - SERVER`() = runTest {
        assertEquals(Result.Error(DataError.Remote.SERVER), errorResult(500))
        assertEquals(Result.Error(DataError.Remote.SERVER), errorResult(503))
        assertEquals(Result.Error(DataError.Remote.SERVER), errorResult(599))
    }

    @Test
    fun `прочие коды ошибки - UNKNOWN`() = runTest {
        assertEquals(Result.Error(DataError.Remote.UNKNOWN), errorResult(404))
        assertEquals(Result.Error(DataError.Remote.UNKNOWN), errorResult(401))
    }

    private suspend fun errorResult(code: Int): Result<String, DataError.Remote> =
        responseToResult(Response.error<String>(code, "".toResponseBody()))
}
