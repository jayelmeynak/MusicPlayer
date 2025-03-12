package com.jayelmeynak.local.data

import android.net.Uri

data class TrackDbo(
    val uri: Uri,
    val id: Long,
    val artist: String,
    val duration: Int,
    val title: String,
)