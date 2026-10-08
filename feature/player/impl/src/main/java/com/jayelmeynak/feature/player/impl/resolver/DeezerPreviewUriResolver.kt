package com.jayelmeynak.feature.player.impl.resolver

import com.jayelmeynak.feature.player.api.TrackUriResolver
import com.jayelmeynak.lib.network.data.RemoteTrackDataSource
import com.jayelmeynak.util.result.onSuccess
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The preview URL of a Deezer track, fetched by id. A preview URL is signed and expires in about
 * 15 minutes, so a URL is cached for [ttlMs] only: a seek or a retry within it does not hit the
 * network, a later load gets a fresh one. Failures are not cached.
 *
 * An adapter over `lib:network` until the catalog feature provides its own resolver.
 */
@Singleton
internal class DeezerPreviewUriResolver(
    private val remoteTrackDataSource: RemoteTrackDataSource,
    private val now: () -> Long,
    private val ttlMs: Long,
) : TrackUriResolver {

    @Inject
    constructor(remoteTrackDataSource: RemoteTrackDataSource) : this(
        remoteTrackDataSource = remoteTrackDataSource,
        now = { System.nanoTime() / NANOS_IN_MILLI },
        ttlMs = DEFAULT_TTL_MS,
    )

    private class CachedUri(val uri: String, val expiresAtMs: Long)

    private val cache = ConcurrentHashMap<String, CachedUri>()

    override suspend fun resolve(id: String): String? {
        val nowMs = now()
        cache[id]?.let { cached ->
            if (nowMs < cached.expiresAtMs) return cached.uri
            // Only this expired entry: a fresh one stored meanwhile by another call stays.
            cache.remove(id, cached)
        }
        var uri: String? = null
        remoteTrackDataSource.getTrack(id).onSuccess { track ->
            uri = track.preview?.takeIf { it.isNotEmpty() }
        }
        uri?.let { cache[id] = CachedUri(it, nowMs + ttlMs) }
        return uri
    }

    companion object {
        /** Well below the ~15-minute lifetime of a signed preview URL. */
        const val DEFAULT_TTL_MS: Long = 10 * 60 * 1000L

        private const val NANOS_IN_MILLI = 1_000_000L
    }
}
