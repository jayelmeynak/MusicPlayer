package com.jayelmeynak.feature.player.impl.domain.usecase

import com.jayelmeynak.lib.mediastore.domain.usecase.GetLocalTracksUseCase
import com.jayelmeynak.feature.player.impl.data.toTrack
import com.jayelmeynak.feature.player.impl.domain.models.Track
import javax.inject.Inject

internal class GetLocalTrackListUseCase @Inject constructor(
    private val getLocalTracksUseCase: GetLocalTracksUseCase
) {
    suspend operator fun invoke(): List<Track> = getLocalTracksUseCase().map { it.toTrack() }
}
