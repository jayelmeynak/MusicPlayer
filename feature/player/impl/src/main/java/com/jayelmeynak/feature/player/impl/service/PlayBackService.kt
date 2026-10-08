package com.jayelmeynak.feature.player.impl.service

import android.app.PendingIntent
import android.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CacheBitmapLoader
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.jayelmeynak.feature.player.impl.artwork.LocalArtworkBitmapLoader
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Owns the player and the media session for its lifetime: both are created in [onCreate] and
 * released in [onDestroy], so a service started again in the same process starts with a new
 * working pair. The app UI reaches them only through a `MediaController`. The standard Media3
 * notification follows the session and puts the service in the foreground while playback is
 * ongoing; nothing calls `startForeground` by hand.
 *
 * [onTaskRemoved] is not overridden on purpose (owner's decision): the Media3 default keeps the
 * service while playing and stops it otherwise.
 */
@UnstableApi
@AndroidEntryPoint
internal class PlayBackService : MediaSessionService() {

    @Inject
    lateinit var playerFactory: PlayerFactory

    @Inject
    lateinit var sessionCallback: PlaybackSessionCallback

    @Inject
    lateinit var bitmapLoader: LocalArtworkBitmapLoader

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        setMediaNotificationProvider(DefaultMediaNotificationProvider.Builder(this).build())
        val player = playerFactory.create()
        setListener(object : Listener {
            /**
             * Android 12+ refuses a foreground start from the background (e.g. a play command
             * from a Bluetooth device long after the app was left). Sound without a notification
             * and a foreground service is not allowed: pause instead.
             */
            override fun onForegroundServiceStartNotAllowedException() {
                Log.w(TAG, "Foreground start not allowed, pausing playback")
                player.pause()
            }
        })
        val session = MediaSession.Builder(this, player)
            .setCallback(sessionCallback)
            .setBitmapLoader(CacheBitmapLoader(bitmapLoader))
            .apply { sessionActivity()?.let(::setSessionActivity) }
            .build()
        mediaSession = session
        // Media3 adds the session when a controller connects through onGetSession; adding it here
        // as well shows the notification even if the service was started without a controller.
        addSession(session)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
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
