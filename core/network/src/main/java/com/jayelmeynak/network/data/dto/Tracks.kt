package com.jayelmeynak.network.data.dto

import com.google.gson.annotations.SerializedName

data class Tracks(
    @SerializedName("data")
    val tracks: List<TrackDto>
)