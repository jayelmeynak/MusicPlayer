package com.jayelmeynak.lib.network.data.dto

import com.google.gson.annotations.SerializedName

public data class Tracks(
    @SerializedName("data")
    public val tracks: List<TrackDto>
)