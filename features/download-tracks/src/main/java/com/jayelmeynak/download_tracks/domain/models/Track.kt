package com.jayelmeynak.download_tracks.domain.models

import android.net.Uri

data class Track(
    val id: Long,
    val title: String,
    val album: Album?,
    val artistName: String,
    val preview: String,
    val duration: Int = 30,
    val uri: Uri
)