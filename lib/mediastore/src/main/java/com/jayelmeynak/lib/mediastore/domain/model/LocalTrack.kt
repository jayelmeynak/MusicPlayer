package com.jayelmeynak.lib.mediastore.domain.model

import android.net.Uri

public data class LocalTrack(
    val id: Long,
    val title: String,
    val artistName: String,
    val duration: Int,
    val uri: Uri,
)
