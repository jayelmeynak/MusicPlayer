package com.jayelmeynak.local.di

import com.jayelmeynak.local.data.LocalTracksRepositoryImpl
import com.jayelmeynak.local.data.source.LocalTracksDataSource
import com.jayelmeynak.local.data.source.LocalTracksDataSourceImpl
import com.jayelmeynak.local.domain.repository.LocalTracksRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class LocalModule {

    @Binds
    @Singleton
    abstract fun bindLocalTracksRepository(
        impl: LocalTracksRepositoryImpl
    ): LocalTracksRepository

    @Binds
    @Singleton
    abstract fun bindLocalTracksDataSource(
        impl: LocalTracksDataSourceImpl
    ): LocalTracksDataSource
}
