package com.jayelmeynak.feature.localmusic.impl.data.source

import com.jayelmeynak.lib.mediastore.data.MediaStoreAudio
import kotlinx.coroutines.flow.Flow

/** Музыка устройства из MediaStore; исключение чтения (например, нет доступа) пробрасывается. */
internal interface LocalTracksDataSource {
    suspend fun getTracksList(): List<MediaStoreAudio>

    /** Сигнал «аудио в MediaStore изменилось»; наблюдение — пока есть подписка. */
    fun audioChanges(): Flow<Unit>
}
