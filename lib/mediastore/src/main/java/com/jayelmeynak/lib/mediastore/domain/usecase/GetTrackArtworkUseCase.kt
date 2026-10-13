package com.jayelmeynak.lib.mediastore.domain.usecase

import com.jayelmeynak.lib.mediastore.domain.repository.ArtworkRepository
import javax.inject.Inject

public class GetTrackArtworkUseCase @Inject constructor(
    private val repository: ArtworkRepository
) {
    public suspend operator fun invoke(trackId: Long, uri: String): ByteArray? =
        repository.getArtwork(trackId, uri)
}
