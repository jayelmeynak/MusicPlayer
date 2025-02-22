package com.jayelmeynak.player.domain.repository

import com.jayelmeynak.player.domain.models.Track

interface MusicLocalRepository {

    fun getTrackByUri(uri: String): Track?

    fun getTracksList(): List<Track>
}