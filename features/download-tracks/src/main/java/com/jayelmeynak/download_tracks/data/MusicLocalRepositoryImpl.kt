package com.jayelmeynak.download_tracks.data

import com.jayelmeynak.download_tracks.domain.MusicLocalRepository
import com.jayelmeynak.download_tracks.domain.models.Track
import com.jayelmeynak.local.data.LocalTracksDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MusicLocalRepositoryImpl @Inject constructor(
    private val localTracksDataSource: LocalTracksDataSource
) : MusicLocalRepository {
    override suspend fun getAudioData(): List<Track> = withContext(Dispatchers.IO) {
        localTracksDataSource.getTracksList().map {
            it.toTrack()
        }
    }
}