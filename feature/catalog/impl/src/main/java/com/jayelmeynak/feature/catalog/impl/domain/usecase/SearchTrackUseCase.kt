package com.jayelmeynak.feature.catalog.impl.domain.usecase

import com.jayelmeynak.feature.catalog.impl.domain.repository.MusicChartsRepository
import javax.inject.Inject

internal class SearchTrackUseCase @Inject constructor(
    private val repository: MusicChartsRepository
) {

    suspend operator fun invoke(q: String) = repository.searchTrack(q)
}
