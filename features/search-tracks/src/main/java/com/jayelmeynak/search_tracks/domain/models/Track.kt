package com.jayelmeynak.search_tracks.domain.models

data class Track(
    val id: Long,
    val title: String,
    val album: Album,
    val artistName: String,
    val preview: String,
    /** Full track length in ms, 0 when unknown; the playable preview is 30 seconds. */
    val duration: Long = 0L,
)
