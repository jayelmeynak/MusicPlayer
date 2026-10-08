package com.jayelmeynak.lib.network.data.dto

import com.google.gson.annotations.SerializedName

public data class AlbumDto(
    @SerializedName("id")
    public val id: Int,
    @SerializedName("title")
    public val title: String,
    @SerializedName("cover")
    public val cover: String,
    @SerializedName("cover_big")
    public val coverBig: String,
    @SerializedName("cover_medium")
    public val coverMedium: String,
    @SerializedName("cover_xl")
    public val coverXl: String,
    @SerializedName("cover_small")
    public val coverSmall: String,
    @SerializedName("tracklist")
    public val trackList: String,
    @SerializedName("type")
    public val type: String
)