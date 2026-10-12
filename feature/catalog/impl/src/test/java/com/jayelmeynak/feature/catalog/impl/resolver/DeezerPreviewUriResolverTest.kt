package com.jayelmeynak.feature.catalog.impl.resolver

import com.jayelmeynak.feature.player.api.TrackSource
import com.jayelmeynak.lib.network.data.dto.TrackDto
import com.jayelmeynak.util.result.DataError
import com.jayelmeynak.util.result.Result
import com.jayelmeynak.lib.network.data.RemoteTrackDataSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
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
    fun `резолвер обслуживает треки Deezer`() {
        assertEquals(TrackSource.DEEZER, resolver.source)
    }

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
    fun `параллельный запрос во время неудачной загрузки получает ту же неудачу, следующий - запрашивает заново`() = runTest {
        val gate = CompletableDeferred<Unit>()
        remote.gates = mapOf("42" to gate)
        remote.tracks = mapOf("42" to Result.Error(DataError.Remote.NO_INTERNET))

        val first = async { resolver.resolve("42") }
        val second = async { resolver.resolve("42") }
        runCurrent()
        gate.complete(Unit)

        assertNull(first.await())
        assertNull(second.await())
        assertEquals(listOf("42"), remote.requestedTracks)

        remote.tracks = mapOf("42" to Result.Success(TrackDto(id = 42, preview = PREVIEW)))
        assertEquals(PREVIEW, resolver.resolve("42"))
        assertEquals(listOf("42", "42"), remote.requestedTracks)
    }

    @Test
    fun `параллельные запросы одного трека - один запрос в сеть, ссылка у всех`() = runTest {
        val gate = CompletableDeferred<Unit>()
        remote.gates = mapOf("42" to gate)
        remote.tracks = mapOf("42" to Result.Success(TrackDto(id = 42, preview = PREVIEW)))

        val requests = List(3) { async { resolver.resolve("42") } }
        runCurrent()
        gate.complete(Unit)

        assertEquals(List(3) { PREVIEW }, requests.map { it.await() })
        assertEquals(listOf("42"), remote.requestedTracks)
    }

    @Test
    fun `разные треки грузятся параллельно, не дожидаясь друг друга`() = runTest {
        val slow = CompletableDeferred<Unit>()
        remote.gates = mapOf("1" to slow)
        remote.tracks = mapOf(
            "1" to Result.Success(TrackDto(id = 1, preview = "p1")),
            "2" to Result.Success(TrackDto(id = 2, preview = "p2")),
        )

        val first = async { resolver.resolve("1") }
        runCurrent()

        assertEquals("p2", resolver.resolve("2"))
        assertFalse(first.isCompleted)
        slow.complete(Unit)
        assertEquals("p1", first.await())
    }

    @Test
    fun `отмена первого запроса - ожидающий запрашивает заново и получает ссылку`() = runTest {
        val gate = CompletableDeferred<Unit>()
        remote.gates = mapOf("42" to gate)
        remote.tracks = mapOf("42" to Result.Success(TrackDto(id = 42, preview = PREVIEW)))

        val owner = async { resolver.resolve("42") }
        runCurrent()
        val waiter = async { resolver.resolve("42") }
        runCurrent()
        owner.cancel()
        runCurrent()
        gate.complete(Unit)

        assertEquals(PREVIEW, waiter.await())
        assertEquals(listOf("42", "42"), remote.requestedTracks)
    }

    @Test
    fun `срок ссылки считается от начала загрузки`() = runTest {
        val gate = CompletableDeferred<Unit>()
        remote.gates = mapOf("42" to gate)
        remote.tracks = mapOf("42" to Result.Success(TrackDto(id = 42, preview = PREVIEW)))
        val startedAt = nowMs

        val first = async { resolver.resolve("42") }
        runCurrent()
        nowMs += 5_000
        gate.complete(Unit)
        assertEquals(PREVIEW, first.await())

        nowMs = startedAt + TTL_MS
        resolver.resolve("42")
        assertEquals(listOf("42", "42"), remote.requestedTracks)
    }

    @Test
    fun `ссылка, полученная между проверкой кэша и захватом загрузки, не запрашивается повторно`() = runTest {
        remote.tracks = mapOf("42" to Result.Success(TrackDto(id = 42, preview = PREVIEW)))
        var interleave: (() -> Unit)? = null
        val racy = DeezerPreviewUriResolver(remote, now = { interleave?.also { interleave = null }?.invoke(); nowMs }, ttlMs = TTL_MS)
        racy.resolve("42")
        nowMs += TTL_MS
        // Пока первый вызов проверяет кэш, второй успевает загрузить свежую ссылку.
        interleave = { runBlocking { racy.resolve("42") } }

        assertEquals(PREVIEW, racy.resolve("42"))
        assertEquals(listOf("42", "42"), remote.requestedTracks)
    }

    @Test
    fun `настоящая ошибка загрузки - ожидающий получает null без повторного запроса`() = runTest {
        val gate = CompletableDeferred<Unit>()
        var calls = 0
        val failing = object : RemoteTrackDataSource by remote {
            override suspend fun getTrack(id: String): Result<TrackDto, DataError.Remote> {
                calls++
                if (calls == 1) {
                    gate.await()
                    throw IllegalStateException("сбой загрузки")
                }
                return Result.Success(TrackDto(id = 42, preview = PREVIEW))
            }
        }
        val shared = DeezerPreviewUriResolver(failing, now = { nowMs }, ttlMs = TTL_MS)

        val owner = async { runCatching { shared.resolve("42") } }
        runCurrent()
        val waiter = async { shared.resolve("42") }
        runCurrent()
        gate.complete(Unit)

        assertTrue(owner.await().exceptionOrNull() is IllegalStateException)
        assertNull(waiter.await())
        assertEquals(1, calls)
    }

    @Test
    fun `отмена ожидающего не отменяет общую загрузку`() = runTest {
        val gate = CompletableDeferred<Unit>()
        remote.gates = mapOf("42" to gate)
        remote.tracks = mapOf("42" to Result.Success(TrackDto(id = 42, preview = PREVIEW)))

        val owner = async { resolver.resolve("42") }
        runCurrent()
        val waiter = async { resolver.resolve("42") }
        runCurrent()
        waiter.cancel()
        runCurrent()
        gate.complete(Unit)

        assertEquals(PREVIEW, owner.await())
        assertEquals(listOf("42"), remote.requestedTracks)
    }

    @Test
    fun `кэш ограничен - новая ссылка вытесняет давно не использованную`() = runTest {
        remote.tracks = (1..3).associate { "$it" to Result.Success(TrackDto(id = it.toLong(), preview = "p$it")) }
        val small = DeezerPreviewUriResolver(remote, now = { nowMs }, ttlMs = TTL_MS, maxEntries = 2)

        small.resolve("1")
        small.resolve("2")
        small.resolve("3")
        remote.requestedTracks.clear()

        small.resolve("2")
        small.resolve("3")
        assertEquals(emptyList<String>(), remote.requestedTracks)
        small.resolve("1")
        assertEquals(listOf("1"), remote.requestedTracks)
    }

    @Test
    fun `обращение к ссылке откладывает её вытеснение`() = runTest {
        remote.tracks = (1..3).associate { "$it" to Result.Success(TrackDto(id = it.toLong(), preview = "p$it")) }
        val small = DeezerPreviewUriResolver(remote, now = { nowMs }, ttlMs = TTL_MS, maxEntries = 2)

        small.resolve("1")
        small.resolve("2")
        small.resolve("1")
        small.resolve("3")
        remote.requestedTracks.clear()

        small.resolve("1")
        assertEquals(emptyList<String>(), remote.requestedTracks)
        small.resolve("2")
        assertEquals(listOf("2"), remote.requestedTracks)
    }

    @Test
    fun `101-я ссылка вытесняет первую`() = runTest {
        remote.tracks = (1..101).associate { "$it" to Result.Success(TrackDto(id = it.toLong(), preview = "p$it")) }

        (1..101).forEach { resolver.resolve("$it") }
        remote.requestedTracks.clear()

        resolver.resolve("2")
        assertEquals(emptyList<String>(), remote.requestedTracks)
        resolver.resolve("1")
        assertEquals(listOf("1"), remote.requestedTracks)
    }

    @Test
    fun `сбой источника и трек без превью пишутся в лог с id трека`() = runTest {
        val lines = mutableListOf<String>()
        val logged = DeezerPreviewUriResolver(remote, now = { nowMs }, ttlMs = TTL_MS, log = { lines += it })
        remote.tracks = mapOf(
            "1" to Result.Error(DataError.Remote.NO_INTERNET),
            "2" to Result.Success(TrackDto(id = 2, preview = null)),
        )

        logged.resolve("1")
        logged.resolve("2")

        assertEquals(2, lines.size)
        assertTrue(lines[0].contains("1") && lines[0].contains("NO_INTERNET"))
        assertTrue(lines[1].contains("2"))
    }

    @Test
    fun `истечение лимита ожидания пишется в лог`() = runTest {
        val lines = mutableListOf<String>()
        val silent = object : RemoteTrackDataSource by remote {
            override suspend fun getTrack(id: String): Result<TrackDto, DataError.Remote> = awaitCancellation()
        }
        val logged = DeezerPreviewUriResolver(silent, now = { nowMs }, ttlMs = TTL_MS, timeoutMs = TIMEOUT_MS, log = { lines += it })

        val result = async { logged.resolve("42") }
        advanceTimeBy(TIMEOUT_MS + 1)

        assertNull(result.await())
        assertEquals(1, lines.size)
        assertTrue(lines.single().contains("42"))
    }

    @Test
    fun `успех и ответ из кэша в лог не пишутся`() = runTest {
        val lines = mutableListOf<String>()
        val logged = DeezerPreviewUriResolver(remote, now = { nowMs }, ttlMs = TTL_MS, log = { lines += it })
        remote.tracks = mapOf("42" to Result.Success(TrackDto(id = 42, preview = PREVIEW)))

        logged.resolve("42")
        logged.resolve("42")

        assertEquals(emptyList<String>(), lines)
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
    fun `лимит ожидания по умолчанию - 10 секунд`() = runTest {
        val silent = object : RemoteTrackDataSource by remote {
            override suspend fun getTrack(id: String): Result<TrackDto, DataError.Remote> = awaitCancellation()
        }
        val injected = DeezerPreviewUriResolver(silent, now = { nowMs }, ttlMs = TTL_MS)

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
