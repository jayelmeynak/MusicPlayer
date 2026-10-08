package com.jayelmeynak.player.domain.usecase

import com.jayelmeynak.util.result.DataError
import com.jayelmeynak.util.result.Result
import com.jayelmeynak.player.domain.models.Track
import com.jayelmeynak.player.domain.repository.MusicRemoteRepository
import javax.inject.Inject

class GetRemoteAlbumUseCase @Inject internal constructor(
    private val musicRemoteRepository: MusicRemoteRepository
) {

    suspend operator fun invoke(id: String): Result<List<Track>, DataError.Remote> {
        return musicRemoteRepository.getAlbum(id)
    }
}