package com.jayelmeynak.feature.localmusic.impl.domain.usecase

import com.jayelmeynak.feature.localmusic.impl.domain.model.LocalTrack
import com.jayelmeynak.feature.localmusic.impl.domain.repository.LocalTracksRepository
import com.jayelmeynak.util.result.DataError
import com.jayelmeynak.util.result.Result
import javax.inject.Inject

internal class GetLocalTracksUseCase @Inject constructor(
    private val repository: LocalTracksRepository
) {
    suspend operator fun invoke(): Result<List<LocalTrack>, DataError.Local> = repository.getTracksList()
}
