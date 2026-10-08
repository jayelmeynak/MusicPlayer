package com.jayelmeynak.lib.mediastore.domain.usecase

import android.net.Uri
import com.jayelmeynak.lib.mediastore.domain.repository.LocalTracksRepository
import javax.inject.Inject

public class GetTrackArtworkUseCase @Inject constructor(
    private val repository: LocalTracksRepository
) {
    public suspend operator fun invoke(trackId: Long, uri: Uri): ByteArray? =
        repository.getArtwork(trackId, uri)
}
