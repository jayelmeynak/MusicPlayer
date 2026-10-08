package com.jayelmeynak.player.di

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.session.MediaSession
import com.jayelmeynak.player.data.MusicRemoteRepositoryImpl
import com.jayelmeynak.player.domain.repository.MusicRemoteRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class PlayerModule {

    @Binds
    @Singleton
    abstract fun bindMusicRemoteRepository(impl: MusicRemoteRepositoryImpl): MusicRemoteRepository

    companion object {

        @Provides
        @Singleton
        fun provideAudioAttributes(): AudioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .setUsage(C.USAGE_MEDIA)
            .build()

        @Provides
        @Singleton
        @UnstableApi
        fun provideExoPlayer(
            @ApplicationContext context: Context,
            audioAttributes: AudioAttributes,
        ): ExoPlayer = ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setTrackSelector(DefaultTrackSelector(context))
            .build()

        @Provides
        @Singleton
        fun provideMediaSession(
            @ApplicationContext context: Context,
            player: ExoPlayer,
        ): MediaSession = MediaSession.Builder(context, player).build()
    }
}
