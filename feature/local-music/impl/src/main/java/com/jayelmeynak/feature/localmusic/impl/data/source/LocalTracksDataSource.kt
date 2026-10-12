package com.jayelmeynak.feature.localmusic.impl.data.source

import com.jayelmeynak.lib.mediastore.data.TrackDbo

internal interface LocalTracksDataSource {
    suspend fun getTracksList(): List<TrackDbo>
}
