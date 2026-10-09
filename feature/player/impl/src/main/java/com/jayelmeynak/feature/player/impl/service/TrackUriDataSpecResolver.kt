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
 * Turns the internal track URI of a session item into a playable one right before the player
 * opens it, so an expiring Deezer URL is fresh for every item and every reopen (seek, retry).
 *
 * Runs on the loader thread, which may block: [runBlocking] waits for the suspending resolver.
 * Interrupting the thread (a cancelled load) cancels the resolver.
 *
 * `null` резолвера (в том числе по его таймауту) — неповторяемая ошибка: плеер сразу встаёт в
 * ошибку источника, без повторных загрузок.
 */
@OptIn(UnstableApi::class)
internal class TrackUriDataSpecResolver @Inject constructor(
    private val resolvers: Map<TrackSource, @JvmSuppressWildcards TrackUriResolver>,
) : ResolvingDataSource.Resolver {

    override fun resolveDataSpec(dataSpec: DataSpec): DataSpec {
        if (dataSpec.uri.scheme != TRACK_URI_SCHEME) return dataSpec
        // Трек недоступен окончательно — FileNotFoundException: политика загрузки Media3 его не
        // повторяет (обычный IOException она повторяет с паузами, и «буферизация» растягивается
        // на несколько таймаутов резолвера). Для плеера это та же ошибка ввода-вывода.
        val key = dataSpec.uri.toTrackKey() ?: throw FileNotFoundException("Unknown track ${dataSpec.uri}")
        val resolver = resolvers[key.source] ?: throw FileNotFoundException("No resolver for ${key.source}")
        val uri = try {
            runBlocking { resolver.resolve(key.id) }
        } catch (e: InterruptedException) {
            // runBlocking сбросил флаг прерывания; вернуть его коду загрузчика выше по стеку.
            Thread.currentThread().interrupt()
            throw InterruptedIOException("Resolving ${key.source} ${key.id} was cancelled")
        } catch (e: IOException) {
            throw e
        } catch (e: Exception) {
            // The contract says resolvers do not throw; a broken one still fails as a load error.
            throw IOException("Resolving ${key.source} ${key.id} failed", e)
        } ?: throw FileNotFoundException("Track ${key.source} ${key.id} is unavailable")
        return dataSpec.withUri(uri.toUri())
    }
}
