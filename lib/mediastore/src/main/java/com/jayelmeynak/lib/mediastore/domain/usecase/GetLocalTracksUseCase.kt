package com.jayelmeynak.lib.mediastore.domain.usecase

import com.jayelmeynak.lib.mediastore.domain.model.LocalTrack
import com.jayelmeynak.lib.mediastore.domain.repository.LocalTracksRepository
import javax.inject.Inject

public class GetLocalTracksUseCase @Inject constructor(
    private val repository: LocalTracksRepository
) {
    public suspend operator fun invoke(): List<LocalTrack> = repository.getTracksList()
}
