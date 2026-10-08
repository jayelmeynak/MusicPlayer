package com.jayelmeynak.feature.player.impl.playback

import android.content.ComponentName
import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.jayelmeynak.feature.player.impl.service.PlayBackService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Starts connecting a new [MediaController] to the playback session; [listener] gets its callbacks. */
internal fun interface MediaControllerConnector {
    fun connect(listener: MediaController.Listener): ListenableFuture<MediaController>
}

/** Connects to [PlayBackService]; binding creates the service if it is not running. */
@OptIn(UnstableApi::class)
internal class SessionServiceConnector @Inject constructor(
    @ApplicationContext private val context: Context,
) : MediaControllerConnector {

    override fun connect(listener: MediaController.Listener): ListenableFuture<MediaController> {
        val token = SessionToken(context, ComponentName(context, PlayBackService::class.java))
        return MediaController.Builder(context, token).setListener(listener).buildAsync()
    }
}
