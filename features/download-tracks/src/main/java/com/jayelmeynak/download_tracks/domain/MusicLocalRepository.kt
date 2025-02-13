package com.jayelmeynak.download_tracks.domain
import com.jayelmeynak.download_tracks.domain.models.Track

interface MusicLocalRepository {

    suspend fun getAudioData(): List<Track>
}