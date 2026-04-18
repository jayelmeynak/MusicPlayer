package com.jayelmeynak.player.domain.useсase

import com.jayelmeynak.network.utils.DataError
import com.jayelmeynak.network.utils.Result
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