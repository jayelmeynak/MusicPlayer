package com.jayelmeynak.local.domain.usecase

import com.jayelmeynak.local.domain.repository.LocalTracksRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class PruneArtworkCacheUseCase @Inject internal constructor(
    private val repository: LocalTracksRepository
) {
    suspend operator fun invoke(activeTrackIds: List<Long>) = withContext(Dispatchers.IO) {
        repository.pruneArtworkCache(activeTrackIds)
    }
}
