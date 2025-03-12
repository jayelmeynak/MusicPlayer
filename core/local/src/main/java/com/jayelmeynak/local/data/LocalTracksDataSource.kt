package com.jayelmeynak.local.data

interface LocalTracksDataSource {
    fun getTrack(uriString: String): TrackDbo?

    fun getTracksList(): List<TrackDbo>
}