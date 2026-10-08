package com.jayelmeynak.lib.network.data.dto

import com.google.gson.annotations.SerializedName

public data class AlbumDto(
    @SerializedName("id")
    public val id: Int? = null,
    @SerializedName("title")
    public val title: String? = null,
    @SerializedName("cover")
    public val cover: String? = null,
    @SerializedName("cover_big")
    public val coverBig: String? = null,
    @SerializedName("cover_medium")
    public val coverMedium: String? = null,
    @SerializedName("cover_xl")
    public val coverXl: String? = null,
    @SerializedName("cover_small")
    public val coverSmall: String? = null,
    @SerializedName("tracklist")
    public val trackList: String? = null,
    @SerializedName("type")
    public val type: String? = null,
)
