package com.jayelmeynak.feature.player.impl.service

import androidx.core.net.toUri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.ResolvingDataSource
import com.jayelmeynak.feature.player.api.TrackSource
import com.jayelmeynak.feature.player.api.TrackUriResolver
import kotlinx.coroutines.runBlocking
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InterruptedIOException
import javax.inject.Inject

/**
 * Превращает внутренний URI трека элемента сессии в играбельный прямо перед тем, как плеер его
 * откроет, — так истекающий URL Deezer свеж для каждого элемента и каждого переоткрытия (перемотка,
 * повтор).
 *
 * Работает на потоке загрузчика, который можно блокировать: [runBlocking] ждёт suspend-резолвер.
 * Прерывание потока (отменённая загрузка) отменяет резолвер.
 *
 * `null` резолвера (в том числе по его таймауту) — неповторяемая ошибка: плеер сразу встаёт в
 * ошибку источника, без повторных загрузок.
 *
 * Набор резолверов раскладывается по источникам при создании ([resolversBySource]): ошибка
 * конфигурации видна при создании сервиса (инъекция Hilt), а не когда пользователь тапает трек.
 */
@OptIn(UnstableApi::class)
internal class TrackUriDataSpecResolver @Inject constructor(
    resolvers: Set<@JvmSuppressWildcards TrackUriResolver>,
) : ResolvingDataSource.Resolver {

    private val resolvers: Map<TrackSource, TrackUriResolver> = resolversBySource(resolvers)

    override fun resolveDataSpec(dataSpec: DataSpec): DataSpec {
        if (dataSpec.uri.scheme != TRACK_URI_SCHEME) return dataSpec
        // Трек недоступен окончательно — FileNotFoundException: политика загрузки Media3 его не
        // повторяет (обычный IOException она повторяет с паузами, и «буферизация» растягивается
        // на несколько таймаутов резолвера). Для плеера это та же ошибка ввода-вывода.
        val key = dataSpec.uri.toTrackKey() ?: throw FileNotFoundException("Unknown track ${dataSpec.uri}")
        val resolver = resolvers.getValue(key.source)
        val uri = try {
            runBlocking { resolver.resolve(key.id) }
        } catch (e: InterruptedException) {
            // runBlocking сбросил флаг прерывания; вернуть его коду загрузчика выше по стеку.
            Thread.currentThread().interrupt()
            throw InterruptedIOException("Resolving ${key.source} ${key.id} was cancelled")
        } catch (e: IOException) {
            throw e
        } catch (e: Exception) {
            // По контракту резолверы не бросают; сломанный всё равно падает как ошибка загрузки.
            throw IOException("Resolving ${key.source} ${key.id} failed", e)
        } ?: throw FileNotFoundException("Track ${key.source} ${key.id} is unavailable")
        return dataSpec.withUri(uri.toUri())
    }
}

/**
 * Раскладывает резолверы по их [TrackUriResolver.source]. У каждого источника должен быть ровно
 * один резолвер: два резолвера одного источника или источник без резолвера —
 * [IllegalStateException] с именем источника.
 */
internal fun resolversBySource(resolvers: Set<TrackUriResolver>): Map<TrackSource, TrackUriResolver> {
    val bySource = resolvers.groupBy { it.source }
    bySource.forEach { (source, sameSource) ->
        check(sameSource.size == 1) {
            "Several resolvers for $source: ${sameSource.joinToString { it::class.java.name }}"
        }
    }
    val missing = TrackSource.entries - bySource.keys
    check(missing.isEmpty()) { "No resolver for $missing" }
    return bySource.mapValues { (_, sameSource) -> sameSource.single() }
}
