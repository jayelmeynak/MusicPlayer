package com.jayelmeynak.local.domain.model

import android.net.Uri

data class LocalTrack(
    val id: Long,
    val title: String,
    val artistName: String,
    val duration: Int,
    val uri: Uri,
)
