package com.jayelmeynak.player.domain.models

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Track(
    val id: Long,
    val title: String,
    val album: Album?,
    val artistName: String,
    val preview: String,
    val duration: Int = 30,
    val uri: Uri?
) : Parcelable