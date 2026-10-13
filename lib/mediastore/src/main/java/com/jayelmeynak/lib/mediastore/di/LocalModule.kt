package com.jayelmeynak.lib.mediastore.di

import com.jayelmeynak.lib.mediastore.data.ContentResolverAudioReader
import com.jayelmeynak.lib.mediastore.data.MediaStoreAudioReader
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class LocalBindsModule {

    @Binds
    abstract fun bindMediaStoreAudioReader(impl: ContentResolverAudioReader): MediaStoreAudioReader
}
