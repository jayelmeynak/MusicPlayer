package com.jayelmeynak.search_tracks.domain.models

data class Track(
    val id: Long,
    val title: String,
    val album: Album,
    val artistName: String,
    val preview: String,
)