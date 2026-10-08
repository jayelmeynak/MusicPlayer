package com.jayelmeynak.lib.mediastore.data

import android.net.Uri

internal data class TrackDbo(
    val uri: Uri,
    val id: Long,
    val artist: String,
    val duration: Int,
    val title: String,
)