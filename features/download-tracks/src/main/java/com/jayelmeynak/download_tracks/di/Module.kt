package com.jayelmeynak.download_tracks.di

import com.jayelmeynak.download_tracks.data.ContentResolverHelper
import com.jayelmeynak.download_tracks.data.MusicLocalRepositoryImpl
import com.jayelmeynak.download_tracks.domain.MusicLocalRepository
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
    fun providesMusicLocalRepository(
        contentResolver: ContentResolverHelper
    ): MusicLocalRepository = MusicLocalRepositoryImpl(contentResolver)

}