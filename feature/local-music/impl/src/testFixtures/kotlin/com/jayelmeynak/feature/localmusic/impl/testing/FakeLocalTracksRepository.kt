package com.jayelmeynak.feature.localmusic.impl.testing

import com.jayelmeynak.feature.localmusic.impl.domain.model.LocalTrack
import com.jayelmeynak.feature.localmusic.impl.domain.repository.LocalTracksRepository
import com.jayelmeynak.util.result.DataError
import com.jayelmeynak.util.result.Result
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/** Репозиторий треков для тестов: отдаёт [tracks] или, если задана, ошибку [tracksError]. */
internal class FakeLocalTracksRepository(
    var tracks: List<LocalTrack> = emptyList(),
) : LocalTracksRepository {

    var tracksRequests = 0

    /** Ошибка чтения, например без разрешения на аудио. */
    var tracksError: DataError.Local? = null

    /** Пока не завершён, ответ задерживается: загрузка «идёт». Ответ — по [tracks]/[tracksError] на момент завершения. */
    var pendingTracks: CompletableDeferred<Unit>? = null

    /** Изменения медиатеки: тест шлёт их сам; число подписчиков — `subscriptionCount`. */
    val changes = MutableSharedFlow<Unit>()

    override suspend fun getTracksList(): Result<List<LocalTrack>, DataError.Local> {
        tracksRequests++
        pendingTracks?.await()
        return tracksError?.let { Result.Error(it) } ?: Result.Success(tracks)
    }

    override fun libraryChanges(): Flow<Unit> = changes
}
