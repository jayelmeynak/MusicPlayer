package com.jayelmeynak.feature.player.impl.domain.models

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class Track(
    val id: Long,
    val title: String,
    val album: Album?,
    val artistName: String,
    val preview: String,
    /** Full track length in ms, 0 when unknown; for Deezer it is the full track, not the preview. */
    val duration: Int = 0,
    val uri: Uri?
) : Parcelable