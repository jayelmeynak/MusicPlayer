package com.jayelmeynak.feature.player.impl.domain.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class Album(
    val id: Int,
    val title: String,
    val cover: String,
    val trackList: String,
    val type: String
) : Parcelable