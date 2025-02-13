package com.jayelmeynak.download_tracks.domain.models

data class Album(
    val id: Int,
    val title: String,
    val cover: String,
    val trackList: String,
    val type: String
)