package com.jayelmeynak.lib.network.data.dto

import com.google.gson.annotations.SerializedName

public data class Artist(
    @SerializedName("id")
    public val id: Int,
    @SerializedName("link")
    public val link: String,
    @SerializedName("name")
    public val name: String,
    @SerializedName("picture")
    public val picture: String,
    @SerializedName("picture_big")
    public val pictureBig: String,
    @SerializedName("picture_medium")
    public val pictureMedium: String,
    @SerializedName("picture_small")
    public val pictureSmall: String,
    @SerializedName("picture_xl")
    public val pictureXl: String,
    @SerializedName("radio")
    public val radio: Boolean,
    @SerializedName("tracklist")
    public val trackList: String,
    @SerializedName("type")
    public val type: String
)