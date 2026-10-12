package com.jayelmeynak.lib.mediastore.domain.usecase

import com.jayelmeynak.lib.mediastore.domain.repository.ArtworkRepository
import javax.inject.Inject

public class PruneArtworkCacheUseCase @Inject constructor(
    private val repository: ArtworkRepository
) {
    public suspend operator fun invoke(activeTrackIds: List<Long>): Unit =
        repository.pruneArtworkCache(activeTrackIds)
}
