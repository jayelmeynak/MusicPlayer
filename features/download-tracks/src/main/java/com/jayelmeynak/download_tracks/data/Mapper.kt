package com.jayelmeynak.download_tracks.data

import com.jayelmeynak.download_tracks.domain.models.Track


fun TrackDbo.toTrack() = Track(
    preview = "",
    title = title,
    id = id,
    artistName = artist,
    duration = duration,
    uri = uri,
    album = null
)
