package com.jayelmeynak.local.domain.usecase

import android.net.Uri
import com.jayelmeynak.local.domain.repository.LocalTracksRepository
import javax.inject.Inject

class GetTrackArtworkUseCase @Inject internal constructor(
    private val repository: LocalTracksRepository
) {
    suspend operator fun invoke(uri: Uri): ByteArray? = repository.getArtwork(uri)
}
