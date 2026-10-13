package com.jayelmeynak.feature.localmusic.impl.testing

import com.jayelmeynak.feature.localmusic.impl.domain.model.LocalTrack
import com.jayelmeynak.feature.localmusic.impl.domain.repository.LocalTracksRepository
import com.jayelmeynak.util.result.DataError
import com.jayelmeynak.util.result.Result

/** Репозиторий треков для тестов: отдаёт [tracks] или, если задана, ошибку [tracksError]. */
internal class FakeLocalTracksRepository(
    var tracks: List<LocalTrack> = emptyList(),
) : LocalTracksRepository {

    var tracksRequests = 0

    /** Ошибка чтения, например без разрешения на аудио. */
    var tracksError: DataError.Local? = null

    override suspend fun getTracksList(): Result<List<LocalTrack>, DataError.Local> {
        tracksRequests++
        return tracksError?.let { Result.Error(it) } ?: Result.Success(tracks)
    }
}
