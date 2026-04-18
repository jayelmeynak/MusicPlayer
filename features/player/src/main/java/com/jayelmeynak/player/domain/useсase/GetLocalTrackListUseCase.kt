package com.jayelmeynak.player.domain.useсase

import com.jayelmeynak.local.domain.usecase.GetLocalTracksUseCase
import com.jayelmeynak.player.data.toTrack
import com.jayelmeynak.player.domain.models.Track
import javax.inject.Inject

class GetLocalTrackListUseCase @Inject constructor(
    private val getLocalTracksUseCase: GetLocalTracksUseCase
) {
    suspend operator fun invoke(): List<Track> = getLocalTracksUseCase().map { it.toTrack() }
}
