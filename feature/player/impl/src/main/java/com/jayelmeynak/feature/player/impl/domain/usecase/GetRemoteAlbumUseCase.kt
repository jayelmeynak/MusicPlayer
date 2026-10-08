package com.jayelmeynak.feature.player.impl.domain.usecase

import com.jayelmeynak.util.result.DataError
import com.jayelmeynak.util.result.Result
import com.jayelmeynak.feature.player.impl.domain.models.Track
import com.jayelmeynak.feature.player.impl.domain.repository.MusicRemoteRepository
import javax.inject.Inject

internal class GetRemoteAlbumUseCase @Inject constructor(
    private val musicRemoteRepository: MusicRemoteRepository
) {

    suspend operator fun invoke(id: String): Result<List<Track>, DataError.Remote> {
        return musicRemoteRepository.getAlbum(id)
    }
}