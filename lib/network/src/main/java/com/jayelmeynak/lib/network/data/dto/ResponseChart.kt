package com.jayelmeynak.lib.network.data.dto

import com.google.gson.annotations.SerializedName

public data class ResponseChart(
    @SerializedName("tracks")
    public val tracks: Tracks
)