package com.jayelmeynak.feature.localmusic.impl.data

import com.jayelmeynak.feature.localmusic.impl.data.source.LocalTracksDataSource
import com.jayelmeynak.feature.localmusic.impl.domain.model.LocalTrack
import com.jayelmeynak.feature.localmusic.impl.domain.repository.LocalTracksRepository
import com.jayelmeynak.lib.mediastore.data.MediaStoreAudio
import com.jayelmeynak.util.result.DataError
import com.jayelmeynak.util.result.Result
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

internal class LocalTracksRepositoryImpl @Inject constructor(
    private val dataSource: LocalTracksDataSource
) : LocalTracksRepository {

    override suspend fun getTracksList(): Result<List<LocalTrack>, DataError.Local> = try {
        Result.Success(dataSource.getTracksList().map { it.toLocalTrack() })
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // Без доступа к аудио MediaStore бросает SecurityException; причина экрану не нужна.
        Result.Error(DataError.Local.UNKNOWN)
    }

    override fun libraryChanges(): Flow<Unit> = dataSource.audioChanges()
}

private fun MediaStoreAudio.toLocalTrack() = LocalTrack(
    id = id,
    title = title,
    artistName = artist,
    duration = durationMs,
    uri = uri,
)
