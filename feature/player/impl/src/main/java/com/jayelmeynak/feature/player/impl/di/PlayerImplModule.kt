package com.jayelmeynak.feature.player.impl.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.jayelmeynak.feature.player.impl.artwork.LocalArtworkSource
import com.jayelmeynak.feature.player.impl.artwork.MediaStoreArtworkSource
import com.jayelmeynak.feature.player.impl.playback.MediaControllerConnector
import com.jayelmeynak.feature.player.impl.playback.ProcessLifecycle
import com.jayelmeynak.feature.player.impl.persistence.DataStorePlaybackStateStore
import com.jayelmeynak.feature.player.impl.persistence.PlaybackStateStore
import com.jayelmeynak.feature.player.impl.persistence.SavedPosition
import com.jayelmeynak.feature.player.impl.persistence.SavedQueue
import com.jayelmeynak.feature.player.impl.persistence.processDataStores
import com.jayelmeynak.feature.player.impl.persistence.savedPositionDataStore
import com.jayelmeynak.feature.player.impl.persistence.savedPositionFile
import com.jayelmeynak.feature.player.impl.persistence.savedQueueDataStore
import com.jayelmeynak.feature.player.impl.persistence.savedQueueFile
import com.jayelmeynak.feature.player.impl.playback.SessionServiceConnector
import com.jayelmeynak.feature.player.impl.service.ExoPlayerFactory
import com.jayelmeynak.feature.player.impl.service.PlayerFactory
import dagger.Binds
import dagger.Module
import dagger.Provides
import com.jayelmeynak.util.coroutines.ApplicationScope
import com.jayelmeynak.util.coroutines.Dispatcher
import com.jayelmeynak.util.coroutines.MusicPlayerDispatchers
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
@OptIn(UnstableApi::class)
internal abstract class PlayerImplModule {

    @Binds
    abstract fun bindPlayerFactory(impl: ExoPlayerFactory): PlayerFactory

    @Binds
    abstract fun bindMediaControllerConnector(impl: SessionServiceConnector): MediaControllerConnector

    @Binds
    abstract fun bindLocalArtworkSource(impl: MediaStoreArtworkSource): LocalArtworkSource

    @Binds
    abstract fun bindPlaybackStateStore(impl: DataStorePlaybackStateStore): PlaybackStateStore

    companion object {

        @Provides
        fun provideAudioAttributes(): AudioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        @Provides
        @ProcessLifecycle
        fun provideProcessLifecycleOwner(): LifecycleOwner = ProcessLifecycleOwner.get()

        // DataStore — один экземпляр на файл в процессе (processDataStores); живёт в скоупе
        // приложения, файловые операции — на IO.
        @Provides
        @Singleton
        fun provideSavedQueueDataStore(
            @ApplicationContext context: Context,
            @ApplicationScope scope: CoroutineScope,
            @Dispatcher(MusicPlayerDispatchers.IO) io: CoroutineDispatcher,
        ): DataStore<SavedQueue> = context.savedQueueFile().let { file ->
            processDataStores.get(file) { savedQueueDataStore(CoroutineScope(scope.coroutineContext + io)) { file } }
        }

        @Provides
        @Singleton
        fun provideSavedPositionDataStore(
            @ApplicationContext context: Context,
            @ApplicationScope scope: CoroutineScope,
            @Dispatcher(MusicPlayerDispatchers.IO) io: CoroutineDispatcher,
        ): DataStore<SavedPosition> = context.savedPositionFile().let { file ->
            processDataStores.get(file) { savedPositionDataStore(CoroutineScope(scope.coroutineContext + io)) { file } }
        }
    }
}
