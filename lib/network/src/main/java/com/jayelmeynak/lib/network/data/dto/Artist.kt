package com.jayelmeynak.lib.network.data.dto

import com.google.gson.annotations.SerializedName

public data class Artist(
    @SerializedName("id")
    public val id: Int? = null,
    @SerializedName("link")
    public val link: String? = null,
    @SerializedName("name")
    public val name: String? = null,
    @SerializedName("picture")
    public val picture: String? = null,
    @SerializedName("picture_big")
    public val pictureBig: String? = null,
    @SerializedName("picture_medium")
    public val pictureMedium: String? = null,
    @SerializedName("picture_small")
    public val pictureSmall: String? = null,
    @SerializedName("picture_xl")
    public val pictureXl: String? = null,
    @SerializedName("radio")
    public val radio: Boolean? = null,
    @SerializedName("tracklist")
    public val trackList: String? = null,
    @SerializedName("type")
    public val type: String? = null,
)
