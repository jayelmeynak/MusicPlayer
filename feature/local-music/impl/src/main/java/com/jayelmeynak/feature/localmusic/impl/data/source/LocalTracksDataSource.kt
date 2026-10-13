package com.jayelmeynak.feature.localmusic.impl.data.source

import com.jayelmeynak.lib.mediastore.data.MediaStoreAudio

/** Музыка устройства из MediaStore; исключение чтения (например, нет доступа) пробрасывается. */
internal interface LocalTracksDataSource {
    suspend fun getTracksList(): List<MediaStoreAudio>
}
