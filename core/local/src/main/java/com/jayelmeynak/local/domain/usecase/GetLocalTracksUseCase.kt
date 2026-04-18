package com.jayelmeynak.local.domain.usecase

import com.jayelmeynak.local.domain.model.LocalTrack
import com.jayelmeynak.local.domain.repository.LocalTracksRepository
import javax.inject.Inject

class GetLocalTracksUseCase @Inject internal constructor(
    private val repository: LocalTracksRepository
) {
    suspend operator fun invoke(): List<LocalTrack> = repository.getTracksList()
}
