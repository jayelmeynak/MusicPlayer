package com.jayelmeynak.local.data.source

import android.net.Uri
import com.jayelmeynak.local.data.TrackDbo

internal interface LocalTracksDataSource {
    suspend fun getTracksList(): List<TrackDbo>

    suspend fun getArtwork(uri: Uri): ByteArray?
}
