package com.jayelmeynak.feature.localmusic.impl.domain.repository

import com.jayelmeynak.feature.localmusic.impl.domain.model.LocalTrack

internal interface LocalTracksRepository {
    suspend fun getTracksList(): List<LocalTrack>
}
