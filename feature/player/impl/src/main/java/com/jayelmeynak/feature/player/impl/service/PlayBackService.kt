package com.jayelmeynak.feature.player.impl.service

import android.app.PendingIntent
import android.util.Log
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Owns the media session for its lifetime: a new session on every [onCreate], released in
 * [onDestroy], so a service started again in the same process never hands out a released session.
 * The standard Media3 notification follows the session and puts the service in the foreground
 * while playback is ongoing; nothing calls `startForeground` by hand.
 *
 * [onTaskRemoved] is not overridden on purpose (owner's decision): the Media3 default keeps the
 * service while playing and stops it otherwise.
 */
@UnstableApi
@AndroidEntryPoint
internal class PlayBackService : MediaSessionService() {

    /** The app-wide player; it outlives the service until the service creates its own player. */
    @Inject
    lateinit var exoPlayer: ExoPlayer

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        setMediaNotificationProvider(DefaultMediaNotificationProvider.Builder(this).build())
        setListener(object : Listener {
            /**
             * Android 12+ refuses a foreground start from the background (e.g. a Deezer track
             * whose network reply arrives after the app was minimized). Sound without a
             * notification and a foreground service is not allowed: pause instead.
             */
            override fun onForegroundServiceStartNotAllowedException() {
                Log.w(TAG, "Foreground start not allowed, pausing playback")
                exoPlayer.pause()
            }
        })
        val session = MediaSession.Builder(this, exoPlayer)
            .apply { sessionActivity()?.let(::setSessionActivity) }
            .build()
        mediaSession = session
        // The app UI connects no controller until it moves to MediaController, so onGetSession
        // may never be called: add the session here or no notification is shown. System
        // controllers (notification, Bluetooth) get this same session from onGetSession.
        addSession(session)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onDestroy() {
        mediaSession?.run {
            // The shared player stays alive after the service: stop it, or it would keep playing
            // without a session, a notification and a foreground service.
            if (player.playbackState != Player.STATE_IDLE) {
                player.playWhenReady = false
                player.stop()
            }
            release()
        }
        mediaSession = null
        clearListener()
        super.onDestroy()
    }

    /** Opens the app as the launcher does; MainActivity is singleTop, so no second copy appears. */
    private fun sessionActivity(): PendingIntent? =
        packageManager.getLaunchIntentForPackage(packageName)?.let { intent ->
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        }

    private companion object {
        const val TAG = "PlayBackService"
    }
}
