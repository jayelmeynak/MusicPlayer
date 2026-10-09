package com.jayelmeynak.feature.player.impl.service

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import java.util.Collections
import javax.inject.Inject

/**
 * Decides who may connect to the session and accepts queue items from controllers.
 *
 * Connections follow [access]: this app and controllers trusted by the system get every command,
 * everyone else is rejected, so no other app reads the queue (the user's library) or drives the
 * player.
 *
 * Items come without a URI (see [toMediaItem]); each one gets the internal track URI, and
 * [TrackUriDataSpecResolver] asks the resolver of its source for the playable URL right before
 * loading it. A URI that comes with an item is never opened.
 */
internal class PlaybackSessionCallback @Inject constructor() : MediaSession.Callback {

    @OptIn(UnstableApi::class)
    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
    ): MediaSession.ConnectionResult = when (controller.access()) {
        ControllerAccess.FULL -> MediaSession.ConnectionResult.AcceptedResultBuilder(session).build()
        ControllerAccess.NONE -> {
            // A platform controller is asked again on every command (e.g. each media key press).
            if (loggedRejectedUids.add(controller.uid)) {
                Log.w(TAG, "Rejected controller ${controller.packageName} (uid=${controller.uid})")
            }
            MediaSession.ConnectionResult.reject()
        }
    }

    override fun onAddMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: MutableList<MediaItem>,
    ): ListenableFuture<MutableList<MediaItem>> =
        Futures.immediateFuture(prepareMediaItems(mediaItems).toMutableList())

    /**
     * An item of this app gets the internal URI of its track. Any other item gets an internal URI
     * that never resolves, even if it carries its own URI: playing a `file://`, an arbitrary
     * `content://` provider or a URL as is would open it with this app's permissions. Loading such
     * an item fails with a player error (the player stops in IDLE with the error, it does not skip
     * to the next item), and the rest of the queue stays in place.
     */
    fun prepareMediaItems(mediaItems: List<MediaItem>): List<MediaItem> = mediaItems.map { item ->
        val uri = item.trackKey()?.toTrackUri()?.toString() ?: UNRESOLVABLE_TRACK_URI
        item.buildUpon().setUri(uri).build()
    }

    private companion object {
        const val TAG = "PlaybackSessionCallback"

        /** Uids whose rejection is already logged in this process: one line per uid, not per key. */
        val loggedRejectedUids: MutableSet<Int> = Collections.synchronizedSet(HashSet())
        const val UNRESOLVABLE_TRACK_URI = "$TRACK_URI_SCHEME://unknown"
    }
}
