package com.jayelmeynak.lib.mediastore.di

import com.jayelmeynak.lib.mediastore.data.ArtworkRepositoryImpl
import com.jayelmeynak.lib.mediastore.data.ContentResolverAudioReader
import com.jayelmeynak.lib.mediastore.data.MediaStoreAudioReader
import com.jayelmeynak.lib.mediastore.domain.repository.ArtworkRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class LocalBindsModule {

    @Binds
    abstract fun bindMediaStoreAudioReader(impl: ContentResolverAudioReader): MediaStoreAudioReader

    @Binds
    @Singleton
    abstract fun bindArtworkRepository(impl: ArtworkRepositoryImpl): ArtworkRepository
}
