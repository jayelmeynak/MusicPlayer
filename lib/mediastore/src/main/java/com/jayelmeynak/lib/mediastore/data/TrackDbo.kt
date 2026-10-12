package com.jayelmeynak.lib.mediastore.data

import android.net.Uri

public data class TrackDbo(
    public val uri: Uri,
    public val id: Long,
    public val artist: String,
    public val duration: Int,
    public val title: String,
)