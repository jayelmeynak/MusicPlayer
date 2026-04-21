package com.jayelmeynak.local.di

import android.content.Context
import androidx.room.Room
import com.jayelmeynak.local.data.LocalTracksRepositoryImpl
import com.jayelmeynak.local.data.db.ArtworkDao
import com.jayelmeynak.local.data.db.LocalDatabase
import com.jayelmeynak.local.data.source.LocalTracksDataSource
import com.jayelmeynak.local.data.source.LocalTracksDataSourceImpl
import com.jayelmeynak.local.domain.repository.LocalTracksRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object LocalDatabaseModule {

    @Provides
    @Singleton
    fun provideLocalDatabase(@ApplicationContext context: Context): LocalDatabase =
        Room.databaseBuilder(
            context,
            LocalDatabase::class.java,
            "local_cache.db",
        ).build()

    @Provides
    @Singleton
    fun provideArtworkDao(db: LocalDatabase): ArtworkDao = db.artworkDao()
}

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
