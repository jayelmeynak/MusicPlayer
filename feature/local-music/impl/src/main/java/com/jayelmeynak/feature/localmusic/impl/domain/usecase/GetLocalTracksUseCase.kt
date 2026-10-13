package com.jayelmeynak.feature.localmusic.impl.domain.usecase

import com.jayelmeynak.feature.localmusic.impl.domain.model.LocalTrack
import com.jayelmeynak.feature.localmusic.impl.domain.repository.LocalTracksRepository
import javax.inject.Inject

internal class GetLocalTracksUseCase @Inject constructor(
    private val repository: LocalTracksRepository
) {
    suspend operator fun invoke(): List<LocalTrack> = repository.getTracksList()
}
