package com.jayelmeynak.feature.localmusic.impl.di

import com.jayelmeynak.feature.localmusic.impl.data.LocalTracksRepositoryImpl
import com.jayelmeynak.feature.localmusic.impl.data.source.LocalTracksDataSource
import com.jayelmeynak.feature.localmusic.impl.data.source.LocalTracksDataSourceImpl
import com.jayelmeynak.feature.localmusic.impl.domain.repository.LocalTracksRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Внутренние привязки локальной музыки; контракты для других модулей — в `LocalMusicModule` (`:di`). */
@Module
@InstallIn(SingletonComponent::class)
internal interface LocalMusicImplModule {

    @Binds
    @Singleton
    fun bindLocalTracksRepository(impl: LocalTracksRepositoryImpl): LocalTracksRepository

    @Binds
    @Singleton
    fun bindLocalTracksDataSource(impl: LocalTracksDataSourceImpl): LocalTracksDataSource
}
