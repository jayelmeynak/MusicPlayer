package com.jayelmeynak.feature.localmusic.impl.data.source

import com.jayelmeynak.lib.mediastore.data.MediaStoreAudio
import com.jayelmeynak.lib.mediastore.data.MediaStoreAudioReader
import com.jayelmeynak.util.coroutines.Dispatcher
import com.jayelmeynak.util.coroutines.MusicPlayerDispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class LocalTracksDataSourceImpl @Inject constructor(
    private val reader: MediaStoreAudioReader,
    @Dispatcher(MusicPlayerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : LocalTracksDataSource {

    override suspend fun getTracksList(): List<MediaStoreAudio> = withContext(ioDispatcher) {
        reader.readAudio()
    }

    // Наблюдатель регистрируется при подписке и снимается при её отмене; важен сам факт изменения,
    // поэтому непрочитанные сигналы склеиваются в один.
    override fun audioChanges(): Flow<Unit> = callbackFlow {
        val observation = reader.observeAudioChanges { trySend(Unit) }
        awaitClose { observation.close() }
    }.conflate()
}
