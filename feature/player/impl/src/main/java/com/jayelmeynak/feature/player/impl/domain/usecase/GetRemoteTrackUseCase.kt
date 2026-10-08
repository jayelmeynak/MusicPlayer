package com.jayelmeynak.feature.player.impl.domain.usecase

import com.jayelmeynak.util.result.DataError
import com.jayelmeynak.util.result.Result
import com.jayelmeynak.feature.player.impl.domain.models.Track
import com.jayelmeynak.feature.player.impl.domain.repository.MusicRemoteRepository
import javax.inject.Inject

internal class GetRemoteTrackUseCase @Inject constructor(
    private val musicRemoteRepository: MusicRemoteRepository
) {

    suspend operator fun invoke(id: String): Result<Track, DataError.Remote> {
        return musicRemoteRepository.getTrack(id)
    }
}