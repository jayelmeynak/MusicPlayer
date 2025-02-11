package com.jayelmeynak.search_tracks.di

import com.jayelmeynak.network.data.RemoteChartDataSource
import com.jayelmeynak.search_tracks.data.MusicChartsRepositoryImpl
import com.jayelmeynak.search_tracks.domain.repositories.MusicChartsRepository
import com.jayelmeynak.search_tracks.domain.usecase.GetChartUseCase
import com.jayelmeynak.search_tracks.domain.usecase.SearchTrackUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object Module {

    @Provides
    @Singleton
    fun provideMusicRepository(remoteMusicDataSource: RemoteChartDataSource): MusicChartsRepository {
        return MusicChartsRepositoryImpl(remoteMusicDataSource)
    }

    @Provides
    @Singleton
    fun provideGetChartUseCase(repository: MusicChartsRepository) = GetChartUseCase(repository)


    @Provides
    @Singleton
    fun provideSearchTrackUseCase(repository: MusicChartsRepository) = SearchTrackUseCase(repository)
}