package com.jayelmeynak.local.di

import com.jayelmeynak.local.data.ContentResolverHelper
import com.jayelmeynak.local.data.LocalTracksDataSource
import com.jayelmeynak.local.data.LocalTracksDataSourceImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object LocalModule {

    @Provides
    @Singleton
    fun provideLocalTracksDataSource(contentResolverHelper: ContentResolverHelper): LocalTracksDataSource {
        return LocalTracksDataSourceImpl(contentResolverHelper)
    }
}