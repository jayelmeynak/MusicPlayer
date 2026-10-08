package com.jayelmeynak.feature.player.api

/**
 * Gives the player a playable URI for a track of one [TrackSource]; the player picks the resolver
 * by [QueueItem.source] and passes [QueueItem.id].
 *
 * The player calls it right before loading each item, off the main thread (on the loader thread),
 * so the result may expire soon after. An implementation may cache URIs for less than their
 * lifetime.
 */
public fun interface TrackUriResolver {

    /**
     * A playable URI for the track [id], or `null` if the track cannot be played now (no network,
     * no access, removed track). Never throws, except [kotlin.coroutines.cancellation.CancellationException].
     */
    public suspend fun resolve(id: String): String?
}
