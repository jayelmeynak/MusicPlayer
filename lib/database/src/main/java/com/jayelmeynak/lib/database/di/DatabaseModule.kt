package com.jayelmeynak.lib.database.di

import android.content.Context
import androidx.room.Room
import com.jayelmeynak.lib.database.data.db.ArtworkDao
import com.jayelmeynak.lib.database.data.db.LOCAL_DATABASE_NAME
import com.jayelmeynak.lib.database.data.db.LocalDatabase
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
            LOCAL_DATABASE_NAME,
        ).build()

    @Provides
    @Singleton
    fun provideArtworkDao(db: LocalDatabase): ArtworkDao = db.artworkDao()
}
