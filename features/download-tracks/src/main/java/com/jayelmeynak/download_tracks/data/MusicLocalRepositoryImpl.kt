package com.jayelmeynak.download_tracks.data

import com.jayelmeynak.download_tracks.domain.MusicLocalRepository
import com.jayelmeynak.download_tracks.domain.models.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MusicLocalRepositoryImpl @Inject constructor(
    private val contentResolver: ContentResolverHelper,
) : MusicLocalRepository {
    override suspend fun getAudioData(): List<Track> = withContext(Dispatchers.IO) {
        contentResolver.getAudioData().map {
            it.toTrack()
        }
    }
}