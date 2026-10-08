package com.jayelmeynak.feature.player.api

/** Where a track comes from and how its id is read. */
public enum class TrackSource {
    /** Deezer catalog; the id is the Deezer track id. */
    DEEZER,

    /** Device audio from MediaStore; the id is the `content://` URI of the track. */
    LOCAL,
}
