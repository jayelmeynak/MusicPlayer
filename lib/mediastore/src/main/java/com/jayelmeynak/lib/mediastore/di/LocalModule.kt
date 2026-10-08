package com.jayelmeynak.lib.mediastore.di

import com.jayelmeynak.lib.mediastore.data.LocalTracksRepositoryImpl
import com.jayelmeynak.lib.mediastore.data.source.LocalTracksDataSource
import com.jayelmeynak.lib.mediastore.data.source.LocalTracksDataSourceImpl
import com.jayelmeynak.lib.mediastore.domain.repository.LocalTracksRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class LocalBindsModule {

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
