package com.jayelmeynak.feature.player.impl.service

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Creates the player of one [PlayBackService] instance; the service releases it. */
internal fun interface PlayerFactory {
    fun create(): ExoPlayer
}

/**
 * Music attributes with audio focus, pause on unplugged headphones, and media loaded through
 * [TrackUriDataSpecResolver], so queue items carry no URL.
 */
@OptIn(UnstableApi::class)
internal class ExoPlayerFactory @Inject constructor(
    @ApplicationContext private val context: Context,
    private val audioAttributes: AudioAttributes,
    private val trackUriResolver: TrackUriDataSpecResolver,
) : PlayerFactory {

    override fun create(): ExoPlayer {
        val dataSourceFactory = ResolvingDataSource.Factory(
            DefaultDataSource.Factory(context),
            trackUriResolver,
        )
        return ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()
    }
}
