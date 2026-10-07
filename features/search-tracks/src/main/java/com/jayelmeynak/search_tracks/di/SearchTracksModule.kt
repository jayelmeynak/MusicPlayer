package com.jayelmeynak.search_tracks.di

import com.jayelmeynak.search_tracks.data.MusicChartsRepositoryImpl
import com.jayelmeynak.search_tracks.domain.repositories.MusicChartsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class SearchTracksModule {

    @Binds
    @Singleton
    abstract fun bindMusicChartsRepository(impl: MusicChartsRepositoryImpl): MusicChartsRepository
}
