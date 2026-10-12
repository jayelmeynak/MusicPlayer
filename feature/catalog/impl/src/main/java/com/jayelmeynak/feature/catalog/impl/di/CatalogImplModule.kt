package com.jayelmeynak.feature.catalog.impl.di

import com.jayelmeynak.feature.catalog.impl.data.MusicChartsRepositoryImpl
import com.jayelmeynak.feature.catalog.impl.domain.repository.MusicChartsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Внутренние привязки каталога; контракты для других модулей — в `CatalogModule` (`:di`). */
@Module
@InstallIn(SingletonComponent::class)
internal interface CatalogImplModule {

    @Binds
    @Singleton
    fun bindMusicChartsRepository(impl: MusicChartsRepositoryImpl): MusicChartsRepository
}
