package com.jayelmeynak.network.data.dto

import com.google.gson.annotations.SerializedName

data class ResponseChart(
    @SerializedName("tracks")
    val tracks: Tracks
)