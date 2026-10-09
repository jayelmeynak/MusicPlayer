package com.jayelmeynak.feature.player.impl.resolver

import com.jayelmeynak.lib.network.data.dto.TrackDto
import com.jayelmeynak.util.result.DataError
import com.jayelmeynak.util.result.Result
import com.jayelmeynak.lib.network.data.RemoteTrackDataSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeezerPreviewUriResolverTest {

    private val remote = FakeRemoteTrackDataSource()
    private var nowMs = 1_000_000L
    private val resolver = DeezerPreviewUriResolver(remote, now = { nowMs }, ttlMs = TTL_MS)

    @Test
    fun `свежий URL превью берётся по id трека`() = runTest {
        remote.tracks = mapOf("42" to Result.Success(TrackDto(id = 42, preview = PREVIEW)))

        assertEquals(PREVIEW, resolver.resolve("42"))
        assertEquals(listOf("42"), remote.requestedTracks)
    }

    @Test
    fun `ошибка сети или трек без превью - null`() = runTest {
        remote.tracks = mapOf(
            "1" to Result.Error(DataError.Remote.NO_INTERNET),
            "2" to Result.Success(TrackDto(id = 2, preview = null)),
            "3" to Result.Success(TrackDto(id = 3, preview = "")),
        )

        assertNull(resolver.resolve("1"))
        assertNull(resolver.resolve("2"))
        assertNull(resolver.resolve("3"))
    }

    @Test
    fun `повтор в пределах TTL не ходит в сеть`() = runTest {
        remote.tracks = mapOf("42" to Result.Success(TrackDto(id = 42, preview = PREVIEW)))

        resolver.resolve("42")
        nowMs += TTL_MS - 1
        val cached = resolver.resolve("42")

        assertEquals(PREVIEW, cached)
        assertEquals(1, remote.requestedTracks.size)
    }

    @Test
    fun `после истечения TTL URL запрашивается заново`() = runTest {
        remote.tracks = mapOf("42" to Result.Success(TrackDto(id = 42, preview = PREVIEW)))
        resolver.resolve("42")
        remote.tracks = mapOf("42" to Result.Success(TrackDto(id = 42, preview = "$PREVIEW&fresh")))

        nowMs += TTL_MS
        val fresh = resolver.resolve("42")

        assertEquals("$PREVIEW&fresh", fresh)
        assertEquals(2, remote.requestedTracks.size)
    }

    @Test
    fun `неудача не кэшируется - следующий вызов снова идёт в сеть`() = runTest {
        remote.tracks = mapOf("42" to Result.Error(DataError.Remote.NO_INTERNET))
        resolver.resolve("42")
        remote.tracks = mapOf("42" to Result.Success(TrackDto(id = 42, preview = PREVIEW)))

        assertEquals(PREVIEW, resolver.resolve("42"))
        assertEquals(2, remote.requestedTracks.size)
    }

    @Test
    fun `неудача не стирает свежий URL, сохранённый параллельным вызовом`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val slowRemote = object : RemoteTrackDataSource by remote {
            var calls = 0
            override suspend fun getTrack(id: String): Result<TrackDto, DataError.Remote> {
                calls++
                return if (calls == 1) {
                    gate.await()
                    Result.Error(DataError.Remote.NO_INTERNET)
                } else {
                    Result.Success(TrackDto(id = 42, preview = PREVIEW))
                }
            }
        }
        val racing = DeezerPreviewUriResolver(slowRemote, now = { nowMs }, ttlMs = TTL_MS)

        val failed = async { racing.resolve("42") }
        runCurrent()
        assertEquals(PREVIEW, racing.resolve("42"))
        gate.complete(Unit)
        assertNull(failed.await())

        assertEquals(PREVIEW, racing.resolve("42"))
        assertEquals(2, slowRemote.calls)
    }

    @Test
    fun `сервер не отвечает - null по истечении лимита, неудача не кэшируется`() = runTest {
        var calls = 0
        val silent = object : RemoteTrackDataSource by remote {
            override suspend fun getTrack(id: String): Result<TrackDto, DataError.Remote> {
                calls++
                awaitCancellation()
            }
        }
        val limited = DeezerPreviewUriResolver(silent, now = { nowMs }, ttlMs = TTL_MS, timeoutMs = TIMEOUT_MS)

        val result = async { limited.resolve("42") }
        advanceTimeBy(TIMEOUT_MS - 1)
        assertFalse(result.isCompleted)
        advanceTimeBy(2)

        assertTrue(result.isCompleted)
        assertNull(result.await())
        val again = async { limited.resolve("42") }
        advanceTimeBy(TIMEOUT_MS + 1)
        assertNull(again.await())
        assertEquals(2, calls)
    }

    @Test
    fun `рабочий конструктор - лимит ожидания 10 секунд`() = runTest {
        val silent = object : RemoteTrackDataSource by remote {
            override suspend fun getTrack(id: String): Result<TrackDto, DataError.Remote> = awaitCancellation()
        }
        val injected = DeezerPreviewUriResolver(silent)

        val result = async { injected.resolve("42") }
        advanceTimeBy(10_000L - 1)
        assertFalse(result.isCompleted)
        advanceTimeBy(2)

        assertTrue(result.isCompleted)
        assertNull(result.await())
    }

    @Test
    fun `TTL по умолчанию короче срока жизни ссылки Deezer`() {
        assertEquals(true, DeezerPreviewUriResolver.DEFAULT_TTL_MS < 15 * 60 * 1000L)
    }

    private companion object {
        const val TTL_MS = 10 * 60 * 1000L
        const val TIMEOUT_MS = 10_000L
        const val PREVIEW = "https://cdns-preview.dzcdn.net/stream/42.mp3?hdnea=exp=1"
    }
}
