package com.jayelmeynak.feature.player.impl.playback

import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.jayelmeynak.feature.player.api.PlaybackError
import com.jayelmeynak.feature.player.api.PlaybackState
import com.jayelmeynak.feature.player.api.QueueItem
import com.jayelmeynak.feature.player.impl.service.toQueueItem

/** The player's items as queue items; `null` for an item this app did not add. */
internal fun Player.readQueue(): List<QueueItem?> =
    (0 until mediaItemCount).map { index -> getMediaItemAt(index).toQueueItem() }

/**
 * The whole player state at once, so the queue, the current index and the flags always agree.
 * [queue] is [readQueue], passed in to rebuild it only when the timeline changes. Foreign items are
 * left out of the queue; if the current item is foreign, [PlaybackState.Active.current] is `null`.
 */
internal fun Player.toPlaybackState(queue: List<QueueItem?> = readQueue()): PlaybackState.Active {
    val items = queue.filterNotNull()
    val index = currentMediaItemIndex
    val currentIndex = when {
        items.isEmpty() -> 0
        queue.getOrNull(index) == null -> -1
        else -> queue.subList(0, index).count { it != null }
    }
    return PlaybackState.Active(
        queue = items,
        currentIndex = currentIndex,
        isPlaying = isPlaying,
        playWhenReady = playWhenReady,
        isBuffering = playbackState == Player.STATE_BUFFERING,
        durationMs = duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: 0L,
        error = playerError?.toPlaybackError(),
    )
}

/** Input/output errors (no URI from the resolver, network, file) mean the track is unavailable. */
internal fun PlaybackException.toPlaybackError(): PlaybackError =
    if (errorCode / ERROR_GROUP == IO_ERROR_GROUP) PlaybackError.SOURCE_UNAVAILABLE else PlaybackError.UNKNOWN

/** Media3 groups error codes by thousands; 2xxx are input/output errors. */
private const val ERROR_GROUP = 1000
private const val IO_ERROR_GROUP = PlaybackException.ERROR_CODE_IO_UNSPECIFIED / ERROR_GROUP
