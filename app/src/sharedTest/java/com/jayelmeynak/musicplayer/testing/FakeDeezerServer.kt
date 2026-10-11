package com.jayelmeynak.musicplayer.testing

import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okio.Buffer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.PI
import kotlin.math.sin

/**
 * Фейковый Deezer API для UI-тестов: чарт, поиск, трек и аудио превью с одного локального сервера.
 *
 * Сервер один на процесс и не останавливается между тестами: сервис плеера и его резолвер живут
 * дольше одного теста и держат адрес, полученный при первой инъекции. [start] нельзя вызывать
 * с main-потока.
 */
object FakeDeezerServer {

    /** Треки чарта, по порядку выдачи. */
    val chartTracks: List<FakeTrack> = (1..5).map { n ->
        FakeTrack(
            id = 1000L + n,
            title = "Тестовый трек $n",
            artist = "Исполнитель $n",
            // Короткое превью — проверка автоперехода на следующий трек.
            previewSeconds = if (n == SHORT_TRACK_NUMBER) SHORT_PREVIEW_SECONDS else PREVIEW_SECONDS,
        )
    }

    /** Треки выдачи поиска по любому запросу. */
    val searchTracks: List<FakeTrack> = (1..2).map { n ->
        FakeTrack(
            id = 2000L + n,
            title = "Найденный трек $n",
            artist = "Найденный исполнитель $n",
            previewSeconds = PREVIEW_SECONDS,
        )
    }

    private val server = MockWebServer()

    @Volatile
    private var started = false

    private val counters = ConcurrentHashMap<String, AtomicInteger>()

    /** Сгенерированные WAV по длительности: 150 с — около 2,4 МБ, на каждый запрос не пересчитываем. */
    private val wavCache = ConcurrentHashMap<Int, ByteArray>()

    /** Адрес сервера без завершающего `/`. */
    val baseUrl: String
        get() {
            check(started) { "FakeDeezerServer.start() не вызван" }
            return server.url("/").toString().trimEnd('/')
        }

    @Synchronized
    fun start() {
        if (started) return
        server.dispatcher = DeezerDispatcher()
        server.start()
        started = true
    }

    /** Сколько раз запрошен путь (без query), например `/chart`. */
    fun requestCount(path: String): Int = counters[path]?.get() ?: 0

    fun resetCounters() {
        counters.clear()
    }

    private class DeezerDispatcher : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
            val path = request.url.encodedPath
            counters.getOrPut(path) { AtomicInteger() }.incrementAndGet()
            return when {
                path == "/chart" -> json("""{"tracks":{"data":[${chartTracks.joinToString(",") { it.json() }}]}}""")
                path == "/search" -> json("""{"data":[${searchTracks.joinToString(",") { it.json() }}]}""")
                path.startsWith("/track/") -> findTrack(path.removePrefix("/track/"))
                    ?.let { json(it.json()) }
                    ?: notFound()
                path.startsWith("/preview/") -> findTrack(path.removePrefix("/preview/").removeSuffix(".wav"))
                    ?.let { audio(it.previewSeconds) }
                    ?: notFound()
                else -> notFound()
            }
        }

        private fun findTrack(id: String): FakeTrack? =
            (chartTracks + searchTracks).firstOrNull { it.id.toString() == id }

        private fun json(body: String) = MockResponse.Builder()
            .code(200)
            .setHeader("Content-Type", "application/json")
            .body(body)
            .build()

        private fun audio(seconds: Int) = MockResponse.Builder()
            .code(200)
            .setHeader("Content-Type", "audio/wav")
            .body(Buffer().write(wavCache.getOrPut(seconds) { wav(seconds) }))
            .build()

        private fun notFound() = MockResponse.Builder().code(404).build()
    }

    /** Трек фейкового Deezer; `preview` указывает на этот же сервер. */
    data class FakeTrack(
        val id: Long,
        val title: String,
        val artist: String,
        val previewSeconds: Int,
    ) {
        fun json(): String =
            """{"id":$id,"title":"$title","artist":{"name":"$artist"},"duration":$TRACK_DURATION_SECONDS,""" +
                """"preview":"$baseUrl/preview/$id.wav"}"""
    }

    /** Полная длина трека в метаданных Deezer, секунды. */
    const val TRACK_DURATION_SECONDS = 30

    /** Длиннее любого теста: позиция не доходит до конца трека, автопереход — только у короткого. */
    private const val PREVIEW_SECONDS = 150
    private const val SHORT_PREVIEW_SECONDS = 3
    private const val SHORT_TRACK_NUMBER = 4
    private const val SAMPLE_RATE = 8_000
    private const val TONE_HZ = 440.0

    /** WAV: PCM 16 бит, моно, тон [TONE_HZ] тихой громкости. */
    private fun wav(seconds: Int): ByteArray {
        val samples = SAMPLE_RATE * seconds
        val dataSize = samples * 2
        val buffer = Buffer()
            .writeUtf8("RIFF").writeIntLe(36 + dataSize).writeUtf8("WAVE")
            .writeUtf8("fmt ").writeIntLe(16).writeShortLe(1).writeShortLe(1)
            .writeIntLe(SAMPLE_RATE).writeIntLe(SAMPLE_RATE * 2).writeShortLe(2).writeShortLe(16)
            .writeUtf8("data").writeIntLe(dataSize)
        for (i in 0 until samples) {
            val value = (sin(2 * PI * TONE_HZ * i / SAMPLE_RATE) * 2_000).toInt()
            buffer.writeShortLe(value)
        }
        return buffer.readByteArray()
    }
}
