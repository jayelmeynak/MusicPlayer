package com.jayelmeynak.local.domain.repository

import android.net.Uri
import com.jayelmeynak.local.domain.model.LocalTrack

internal interface LocalTracksRepository {
    suspend fun getTracksList(): List<LocalTrack>

    suspend fun getArtwork(uri: Uri): ByteArray?
}
