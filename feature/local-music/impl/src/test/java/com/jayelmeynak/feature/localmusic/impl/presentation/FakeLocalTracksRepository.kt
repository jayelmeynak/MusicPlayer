package com.jayelmeynak.feature.localmusic.impl.presentation

import com.jayelmeynak.feature.localmusic.impl.domain.model.LocalTrack
import com.jayelmeynak.feature.localmusic.impl.domain.repository.LocalTracksRepository

internal class FakeLocalTracksRepository(
    var tracks: List<LocalTrack> = emptyList(),
) : LocalTracksRepository {

    var tracksRequests = 0

    /** Бросается из getTracksList, например SecurityException без разрешения на аудио. */
    var tracksError: Exception? = null

    override suspend fun getTracksList(): List<LocalTrack> {
        tracksRequests++
        tracksError?.let { throw it }
        return tracks
    }
}
