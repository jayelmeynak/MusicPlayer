package com.jayelmeynak.feature.player.impl.di

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.jayelmeynak.feature.player.api.TrackSource
import com.jayelmeynak.feature.player.api.TrackUriResolver
import com.jayelmeynak.feature.player.impl.artwork.LocalArtworkSource
import com.jayelmeynak.feature.player.impl.artwork.MediaStoreArtworkSource
import com.jayelmeynak.feature.player.impl.playback.MediaControllerConnector
import com.jayelmeynak.feature.player.impl.playback.ProcessLifecycle
import com.jayelmeynak.feature.player.impl.playback.SessionServiceConnector
import com.jayelmeynak.feature.player.impl.resolver.DeezerPreviewUriResolver
import com.jayelmeynak.feature.player.impl.resolver.LocalContentUriResolver
import com.jayelmeynak.feature.player.impl.resolver.TrackSourceKey
import com.jayelmeynak.feature.player.impl.service.ExoPlayerFactory
import com.jayelmeynak.feature.player.impl.service.PlayerFactory
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
@OptIn(UnstableApi::class)
internal abstract class PlayerImplModule {

    @Binds
    abstract fun bindPlayerFactory(impl: ExoPlayerFactory): PlayerFactory

    @Binds
    abstract fun bindMediaControllerConnector(impl: SessionServiceConnector): MediaControllerConnector

    @Binds
    abstract fun bindLocalArtworkSource(impl: MediaStoreArtworkSource): LocalArtworkSource

    @Binds
    @IntoMap
    @TrackSourceKey(TrackSource.DEEZER)
    abstract fun bindDeezerResolver(impl: DeezerPreviewUriResolver): TrackUriResolver

    @Binds
    @IntoMap
    @TrackSourceKey(TrackSource.LOCAL)
    abstract fun bindLocalResolver(impl: LocalContentUriResolver): TrackUriResolver

    companion object {

        @Provides
        fun provideAudioAttributes(): AudioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        @Provides
        @ProcessLifecycle
        fun provideProcessLifecycleOwner(): LifecycleOwner = ProcessLifecycleOwner.get()
    }
}
