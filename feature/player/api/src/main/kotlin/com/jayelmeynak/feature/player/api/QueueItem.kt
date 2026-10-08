package com.jayelmeynak.feature.player.api

/**
 * One track of the playback queue.
 *
 * Holds what identifies the track and what the player shows, never a stream URL: a Deezer preview
 * URL expires in about 15 minutes, so the player asks [TrackUriResolver] for a fresh one right
 * before loading the track.
 *
 * @property id Track id in [source]; see [TrackSource] for its format.
 * @property artworkUri Cover image URI, or `null` if there is none.
 * @property durationMs Full length of the track in milliseconds; `0` when unknown.
 */
public data class QueueItem(
    val id: String,
    val source: TrackSource,
    val title: String,
    val artist: String,
    val artworkUri: String?,
    val durationMs: Long,
)
