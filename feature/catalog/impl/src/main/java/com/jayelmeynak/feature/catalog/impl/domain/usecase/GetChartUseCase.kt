package com.jayelmeynak.feature.catalog.impl.domain.usecase

import com.jayelmeynak.feature.catalog.impl.domain.repository.MusicChartsRepository
import javax.inject.Inject

internal class GetChartUseCase @Inject constructor(
    private val repository: MusicChartsRepository
)
{
    suspend operator fun invoke() = repository.getChart()
}
