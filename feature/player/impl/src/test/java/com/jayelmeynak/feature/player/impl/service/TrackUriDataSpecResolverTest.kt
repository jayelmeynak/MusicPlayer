package com.jayelmeynak.feature.player.impl.service

import android.net.Uri
import androidx.media3.datasource.DataSpec
import com.jayelmeynak.feature.player.api.TrackSource
import com.jayelmeynak.feature.player.api.testing.FakeTrackUriResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import com.jayelmeynak.feature.player.api.TrackUriResolver
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.currentCoroutineContext
import org.junit.Assert.assertTrue
import java.io.IOException
import java.io.InterruptedIOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/** Robolectric: DataSpec holds an android.net.Uri. */
@RunWith(RobolectricTestRunner::class)
class TrackUriDataSpecResolverTest {

    private val deezer = FakeTrackUriResolver(mapOf("42" to "https://cdn.example/42.mp3?exp=1"))
    private val local = FakeTrackUriResolver(mapOf(LOCAL_ID to LOCAL_ID))
    private val resolver = TrackUriDataSpecResolver(
        mapOf(TrackSource.DEEZER to deezer, TrackSource.LOCAL to local),
    )

    @Test
    fun `внутренний URI получает URL от резолвера своего источника`() {
        val resolved = resolver.resolveDataSpec(spec(TrackKey(TrackSource.DEEZER, "42")))

        assertEquals(Uri.parse("https://cdn.example/42.mp3?exp=1"), resolved.uri)
        assertEquals(listOf("42"), deezer.requested)
        assertEquals(emptyList<String>(), local.requested)
    }

    @Test
    fun `локальный трек получает свой content URI, позиция DataSpec сохраняется`() {
        val spec = spec(TrackKey(TrackSource.LOCAL, LOCAL_ID)).subrange(1_000)

        val resolved = resolver.resolveDataSpec(spec)

        assertEquals(Uri.parse(LOCAL_ID), resolved.uri)
        assertEquals(1_000L, resolved.position)
    }

    @Test
    fun `каждое открытие источника спрашивает резолвер заново`() {
        val spec = spec(TrackKey(TrackSource.DEEZER, "42"))

        resolver.resolveDataSpec(spec)
        resolver.resolveDataSpec(spec.subrange(500))

        assertEquals(listOf("42", "42"), deezer.requested)
    }

    @Test(expected = IOException::class)
    fun `резолвер вернул null - IOException`() {
        resolver.resolveDataSpec(spec(TrackKey(TrackSource.DEEZER, "missing")))
    }

    @Test(expected = IOException::class)
    fun `нет резолвера для источника - IOException`() {
        TrackUriDataSpecResolver(emptyMap()).resolveDataSpec(spec(TrackKey(TrackSource.LOCAL, LOCAL_ID)))
    }

    @Test(expected = IOException::class)
    fun `битый внутренний URI - IOException`() {
        resolver.resolveDataSpec(DataSpec(Uri.parse("$TRACK_URI_SCHEME://NOPE/1")))
    }

    @Test
    fun `исключение резолвера - IOException с причиной`() {
        val failure = IllegalStateException("boom")
        val failing = TrackUriDataSpecResolver(mapOf(TrackSource.DEEZER to TrackUriResolver { throw failure }))

        val error = runCatching { failing.resolveDataSpec(spec(TrackKey(TrackSource.DEEZER, "42"))) }
            .exceptionOrNull()

        assertTrue(error is IOException)
        assertSame(failure, error?.cause)
    }

    @Test
    fun `прерывание потока загрузчика - InterruptedIOException, резолвер отменён`() {
        val started = CountDownLatch(1)
        var resolveJob: Job? = null
        val hanging = TrackUriResolver {
            resolveJob = currentCoroutineContext()[Job]
            started.countDown()
            awaitCancellation()
        }
        val blocking = TrackUriDataSpecResolver(mapOf(TrackSource.DEEZER to hanging))
        var error: Throwable? = null
        val loader = thread {
            error = runCatching { blocking.resolveDataSpec(spec(TrackKey(TrackSource.DEEZER, "42"))) }
                .exceptionOrNull()
        }
        assertTrue(started.await(5, TimeUnit.SECONDS))

        loader.interrupt()
        loader.join(5_000)

        assertTrue(error is InterruptedIOException)
        assertTrue(resolveJob?.isCancelled == true)
    }

    @Test
    fun `обычный URI проходит без изменений`() {
        val spec = DataSpec(Uri.parse("https://example.com/1.mp3"))

        assertSame(spec, resolver.resolveDataSpec(spec))
    }

    private fun spec(key: TrackKey) = DataSpec(key.toTrackUri())

    private companion object {
        const val LOCAL_ID = "content://media/external/audio/media/7"
    }
}
