package com.jayelmeynak.feature.localmusic.impl.domain.repository

import com.jayelmeynak.feature.localmusic.impl.domain.model.LocalTrack
import com.jayelmeynak.util.result.DataError
import com.jayelmeynak.util.result.Result
import kotlinx.coroutines.flow.Flow

internal interface LocalTracksRepository {
    suspend fun getTracksList(): Result<List<LocalTrack>, DataError.Local>

    /** Медиатека изменилась (трек добавлен, изменён или удалён); наблюдение — пока есть подписка. */
    fun libraryChanges(): Flow<Unit>
}
