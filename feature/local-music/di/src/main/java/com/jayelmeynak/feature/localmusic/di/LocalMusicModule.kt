package com.jayelmeynak.feature.localmusic.di

import com.jayelmeynak.feature.localmusic.impl.navigation.LocalMusicEntryInstaller
import com.jayelmeynak.feature.localmusic.impl.resolver.LocalContentUriResolver
import com.jayelmeynak.feature.player.api.TrackUriResolver
import com.jayelmeynak.lib.navigation.EntryInstaller
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/** Связывает локальную музыку с приложением: экран вкладки «Local» и резолвер локальных треков для плеера. */
@Module
@InstallIn(SingletonComponent::class)
public interface LocalMusicModule {

    @Binds
    @IntoSet
    public fun bindLocalMusicEntryInstaller(impl: LocalMusicEntryInstaller): EntryInstaller

    @Binds
    @IntoSet
    public fun bindLocalResolver(impl: LocalContentUriResolver): TrackUriResolver
}
