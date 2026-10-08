package com.jayelmeynak.feature.player.di

import com.jayelmeynak.feature.player.api.MiniPlayerHost
import com.jayelmeynak.feature.player.api.PlaybackController
import com.jayelmeynak.feature.player.impl.navigation.PlayerEntryInstaller
import com.jayelmeynak.feature.player.impl.playback.MediaControllerPlaybackController
import com.jayelmeynak.feature.player.impl.presentation.MiniPlayerHostImpl
import com.jayelmeynak.lib.navigation.EntryInstaller
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/** Binds the player contracts from `:api` to their implementations. */
@Module
@InstallIn(SingletonComponent::class)
public interface PlayerModule {

    @Binds
    public fun bindPlaybackController(impl: MediaControllerPlaybackController): PlaybackController

    @Binds
    public fun bindMiniPlayerHost(impl: MiniPlayerHostImpl): MiniPlayerHost

    @Binds
    @IntoSet
    public fun bindPlayerEntryInstaller(impl: PlayerEntryInstaller): EntryInstaller
}
