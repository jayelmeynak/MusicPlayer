package com.jayelmeynak.player.domain.useсase

import com.jayelmeynak.network.utils.DataError
import com.jayelmeynak.network.utils.Result
import com.jayelmeynak.player.domain.models.Track
import com.jayelmeynak.player.domain.repository.MusicRemoteRepository
import javax.inject.Inject

class GetRemoteTrackUseCase @Inject internal constructor(
    private val musicRemoteRepository: MusicRemoteRepository
) {

    suspend operator fun invoke(id: String): Result<Track, DataError.Remote> {
        return musicRemoteRepository.getTrack(id)
    }
}