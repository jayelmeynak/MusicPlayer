package com.jayelmeynak.feature.localmusic.impl.domain.model

import android.net.Uri

internal data class LocalTrack(
    val id: Long,
    val title: String,
    val artistName: String,
    val duration: Int,
    val uri: Uri,
)
