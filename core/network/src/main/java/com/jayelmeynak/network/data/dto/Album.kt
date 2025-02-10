package com.jayelmeynak.network.data.dto

import com.google.gson.annotations.SerializedName

data class Album(
    @SerializedName("id")
    val id: Int,
    @SerializedName("title")
    val title: String,
    @SerializedName("cover")
    val cover: String,
    @SerializedName("cover_big")
    val coverBig: String,
    @SerializedName("cover_medium")
    val coverMedium: String,
    @SerializedName("cover_xl")
    val coverXl: String,
    @SerializedName("cover_small")
    val coverSmall: String,
    @SerializedName("tracklist")
    val trackList: String,
    @SerializedName("type")
    val type: String
)