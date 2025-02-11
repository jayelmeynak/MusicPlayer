package com.jayelmeynak.search_tracks.domain.usecase

import com.jayelmeynak.search_tracks.domain.repositories.MusicChartsRepository
import javax.inject.Inject

class SearchTrackUseCase @Inject constructor(
    private val repository: MusicChartsRepository
) {

    suspend fun invoke(q: String) = repository.searchTrack(q)
}