package com.jayelmeynak.feature.player.api

/**
 * Gives the player a playable URI for a track of one [TrackSource]; the player picks the resolver
 * by [QueueItem.source] and passes [QueueItem.id].
 *
 * The player calls it right before loading each item, off the main thread (on the loader thread),
 * so the result may expire soon after. An implementation may cache URIs for less than their
 * lifetime.
 *
 * Резолверы подключаются к плееру набором (`@Binds @IntoSet` в Hilt-модуле); плеер раскладывает
 * их по [source] при создании и падает, если у источника нет резолвера или их два.
 */
public interface TrackUriResolver {

    /** Источник треков, который обслуживает резолвер; у каждого источника ровно один резолвер. */
    public val source: TrackSource

    /**
     * A playable URI for the track [id], or `null` if the track cannot be played now (no network,
     * no access, removed track). Never throws, except [kotlin.coroutines.cancellation.CancellationException].
     */
    public suspend fun resolve(id: String): String?
}
