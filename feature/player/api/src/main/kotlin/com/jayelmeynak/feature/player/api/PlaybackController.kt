package com.jayelmeynak.feature.player.api

import kotlinx.coroutines.flow.StateFlow

/**
 * Controls the playback session that the player service owns.
 *
 * [state] is [PlaybackState.Idle] until the controller connects to the session. [play] called
 * before that runs once connected (the last call wins); the transport commands ([togglePlayPause],
 * [seekTo], [next], [previous], [seekBack], [seekForward]) are ignored while idle.
 */
public interface PlaybackController {

    public val state: StateFlow<PlaybackState>

    /**
     * Position in the current item in milliseconds. Updated while playing, and once after a seek
     * on pause and after the current item changes; [play] resets it to `0`.
     */
    public val positionMs: StateFlow<Long>

    /**
     * Replaces the queue with [queue] and starts playing the item at [startIndex] from the start.
     *
     * @throws IllegalArgumentException if [startIndex] is not in `queue.indices` (so [queue] must
     *   not be empty).
     */
    public fun play(queue: List<QueueItem>, startIndex: Int)

    public fun togglePlayPause()

    public fun seekTo(positionMs: Long)

    public fun next()

    public fun previous()

    public fun seekBack()

    public fun seekForward()
}
