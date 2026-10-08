package com.jayelmeynak.feature.player.impl.service

import androidx.media3.common.MediaItem
import androidx.media3.session.MediaSession
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import javax.inject.Inject

/**
 * Accepts queue items from controllers. Items come without a URI (see [toMediaItem]); each one
 * gets the internal track URI, and [TrackUriDataSpecResolver] asks the resolver of its source for
 * the playable URL right before loading it.
 */
internal class PlaybackSessionCallback @Inject constructor() : MediaSession.Callback {

    override fun onAddMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: MutableList<MediaItem>,
    ): ListenableFuture<MutableList<MediaItem>> =
        Futures.immediateFuture(prepareMediaItems(mediaItems).toMutableList())

    /**
     * A foreign item with its own URI is kept as is; one without a URI gets an internal URI that
     * never resolves: loading it fails with a player error (the player stops in IDLE with the
     * error, it does not skip to the next item), and the rest of the queue stays in place.
     */
    fun prepareMediaItems(mediaItems: List<MediaItem>): List<MediaItem> = mediaItems.map { item ->
        val key = item.trackKey()
        when {
            key != null -> item.buildUpon().setUri(key.toTrackUri()).build()
            item.localConfiguration != null -> item
            else -> item.buildUpon().setUri(UNRESOLVABLE_TRACK_URI).build()
        }
    }

    private companion object {
        const val UNRESOLVABLE_TRACK_URI = "$TRACK_URI_SCHEME://unknown"
    }
}
