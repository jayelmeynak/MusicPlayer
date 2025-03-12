package com.jayelmeynak.player.di

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.session.MediaSession
import com.jayelmeynak.local.data.LocalTracksDataSource
import com.jayelmeynak.network.data.RemoteTrackDataSource
import com.jayelmeynak.player.data.MusicLocalRepositoryImpl
import com.jayelmeynak.player.data.MusicRemoteRepositoryImpl
import com.jayelmeynak.player.domain.repository.MusicLocalRepository
import com.jayelmeynak.player.domain.repository.MusicRemoteRepository
import com.jayelmeynak.player.player.notification.MusicNotificationManager
import com.jayelmeynak.player.player.service.MusicServiceHandler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object Module {

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

    @Provides
    @Singleton
    fun provideNotificationManager(
        @ApplicationContext context: Context,
        player: ExoPlayer,
    ): MusicNotificationManager = MusicNotificationManager(
        context = context,
        exoPlayer = player
    )

    @Provides
    @Singleton
    fun provideMusicRemoteRepository(remoteTrackDataSource: RemoteTrackDataSource): MusicRemoteRepository {
        return MusicRemoteRepositoryImpl(remoteTrackDataSource)
    }

    @Provides
    @Singleton
    fun provideMusicLocalRepository(localTracksDataSource: LocalTracksDataSource): MusicLocalRepository{
        return MusicLocalRepositoryImpl(localTracksDataSource)
    }


    @Provides
    @Singleton
    fun provideServiceHandler(exoPlayer: ExoPlayer): MusicServiceHandler =
        MusicServiceHandler(exoPlayer)

}
