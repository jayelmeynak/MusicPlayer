package com.jayelmeynak.feature.catalog.impl.resolver

import android.util.Log
import com.jayelmeynak.feature.player.api.TrackSource
import com.jayelmeynak.feature.player.api.TrackUriResolver
import com.jayelmeynak.lib.network.data.RemoteTrackDataSource
import com.jayelmeynak.util.result.onError
import com.jayelmeynak.util.result.onSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ссылка на превью трека Deezer по id. Ссылка подписана и живёт ~15 минут, поэтому кэшируется на
 * [ttlMs]: перемотка или повтор в это время не ходят в сеть, позже приходит свежая. Кэш ограничен
 * [maxEntries] записями — при переполнении вытесняется давно не использованная. Неудачи не
 * кэшируются.
 *
 * Параллельные запросы одного трека делят одну загрузку. Ожидание ответа ограничено [timeoutMs]:
 * по истечении — `null`, трек недоступен. Сбой и истечение лимита пишутся в [log].
 *
 * Резолвер источника `DEEZER` для плеера; `public` только ради привязки `@IntoSet` в `:di`.
 */
@Singleton
public class DeezerPreviewUriResolver internal constructor(
    private val remoteTrackDataSource: RemoteTrackDataSource,
    private val now: () -> Long,
    private val ttlMs: Long,
    private val timeoutMs: Long = DEFAULT_TIMEOUT_MS,
    private val maxEntries: Int = DEFAULT_MAX_ENTRIES,
    private val log: (String) -> Unit = {},
) : TrackUriResolver {

    @Inject
    internal constructor(remoteTrackDataSource: RemoteTrackDataSource) : this(
        remoteTrackDataSource = remoteTrackDataSource,
        now = { System.nanoTime() / NANOS_IN_MILLI },
        ttlMs = DEFAULT_TTL_MS,
        log = { message -> Log.w(TAG, message) },
    )

    override val source: TrackSource = TrackSource.DEEZER

    private class CachedUri(val uri: String, val expiresAtMs: Long)

    /** Чем закончилась проверка кэша: ссылка есть, ждём чужую загрузку или грузим сами. */
    private sealed interface Claim {
        class Hit(val uri: String) : Claim
        class Wait(val running: CompletableDeferred<String?>) : Claim
        class Own(val mine: CompletableDeferred<String?>, val startedAtMs: Long) : Claim
    }

    // Кэш и загрузки в работе — под одним монитором: проверка кэша и захват загрузки атомарны.
    private val lock = Any()

    // Порядок обращений: первой вытесняется давно не использованная ссылка.
    private val cache = object : LinkedHashMap<String, CachedUri>(INITIAL_CAPACITY, LOAD_FACTOR, ACCESS_ORDER) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CachedUri>): Boolean =
            size > maxEntries
    }

    /** Загрузки в работе: параллельный запрос того же трека ждёт уже идущую. */
    private val inFlight = HashMap<String, CompletableDeferred<String?>>()

    override suspend fun resolve(id: String): String? {
        while (true) {
            when (val claim = claim(id)) {
                is Claim.Hit -> return claim.uri
                is Claim.Own -> return load(id, claim.mine, claim.startedAtMs)
                is Claim.Wait -> try {
                    return claim.running.await()
                } catch (cancelled: CancellationException) {
                    // Отменили нас — пробрасываем; отменили чужую загрузку — запрашиваем сами.
                    currentCoroutineContext().ensureActive()
                } catch (failure: Throwable) {
                    // Чужая загрузка упала: та же неудача, без повторного запроса.
                    return null
                }
            }
        }
    }

    private fun claim(id: String): Claim = synchronized(lock) {
        // Время — до чтения кэша: всё, что записано к этому моменту, будет видно.
        val nowMs = now()
        val entry = cache[id]
        if (entry != null) {
            if (nowMs < entry.expiresAtMs) return Claim.Hit(entry.uri)
            cache.remove(id)
        }
        inFlight[id]?.let { return Claim.Wait(it) }
        val mine = CompletableDeferred<String?>()
        inFlight[id] = mine
        Claim.Own(mine, nowMs)
    }

    private suspend fun load(id: String, mine: CompletableDeferred<String?>, startedAtMs: Long): String? {
        try {
            val uri = fetch(id)
            synchronized(lock) {
                // Срок — от начала загрузки: ссылка подписана не позже, чем ушёл запрос.
                if (uri != null) cache[id] = CachedUri(uri, startedAtMs + ttlMs)
                inFlight.remove(id)
            }
            mine.complete(uri)
            return uri
        } catch (cancelled: CancellationException) {
            release(id)
            // Ожидающие не получают чужую отмену как результат: повторят запрос сами.
            mine.cancel(cancelled)
            throw cancelled
        } catch (failure: Throwable) {
            release(id)
            mine.completeExceptionally(failure)
            throw failure
        }
    }

    private fun release(id: String) {
        synchronized(lock) { inFlight.remove(id) }
    }

    private suspend fun fetch(id: String): String? {
        // Не ответил за timeoutMs — трек недоступен, как при ошибке сети.
        val result = withTimeoutOrNull(timeoutMs) { remoteTrackDataSource.getTrack(id) }
        if (result == null) {
            log("Ссылка на превью трека $id не получена: нет ответа за $timeoutMs мс")
            return null
        }
        var uri: String? = null
        result
            .onSuccess { track ->
                uri = track.preview?.takeIf { it.isNotEmpty() }
                if (uri == null) log("У трека $id нет ссылки на превью")
            }
            .onError { error -> log("Ссылка на превью трека $id не получена: $error") }
        return uri
    }

    internal companion object {
        /** Заметно меньше ~15 минут жизни подписанной ссылки на превью. */
        const val DEFAULT_TTL_MS: Long = 10 * 60 * 1000L

        /**
         * Общий лимит на получение ссылки: без него поток загрузчика ждёт ответа до двух
         * сетевых таймаутов подряд, а экран всё это время показывает буферизацию.
         */
        const val DEFAULT_TIMEOUT_MS: Long = 10_000L

        /** Хватает на несколько очередей (чарт — 10 треков, выдача поиска — до 25). */
        const val DEFAULT_MAX_ENTRIES: Int = 100

        // Значения HashMap по умолчанию; ACCESS_ORDER — порядок обращений вместо порядка вставки.
        private const val INITIAL_CAPACITY = 16
        private const val LOAD_FACTOR = 0.75f
        private const val ACCESS_ORDER = true

        private const val TAG = "DeezerPreviewUriResolver"
        private const val NANOS_IN_MILLI = 1_000_000L
    }
}
