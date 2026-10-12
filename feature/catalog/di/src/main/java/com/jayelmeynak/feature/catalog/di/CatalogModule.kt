package com.jayelmeynak.feature.catalog.di

import com.jayelmeynak.feature.catalog.impl.navigation.CatalogEntryInstaller
import com.jayelmeynak.feature.catalog.impl.resolver.DeezerPreviewUriResolver
import com.jayelmeynak.feature.player.api.TrackUriResolver
import com.jayelmeynak.lib.navigation.EntryInstaller
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/** Связывает каталог с приложением: экран каталога и резолвер ссылок превью Deezer для плеера. */
@Module
@InstallIn(SingletonComponent::class)
public interface CatalogModule {

    @Binds
    @IntoSet
    public fun bindCatalogEntryInstaller(impl: CatalogEntryInstaller): EntryInstaller

    @Binds
    @IntoSet
    public fun bindDeezerResolver(impl: DeezerPreviewUriResolver): TrackUriResolver
}
