package com.jayelmeynak.local.data

import javax.inject.Inject

class LocalTracksDataSourceImpl @Inject constructor(
    private val contentResolverHelper: ContentResolverHelper
) : LocalTracksDataSource {
    override fun getTrack(uriString: String): TrackDbo? {
        return contentResolverHelper.getTrackByUri(uriString)
    }

    override fun getTracksList(): List<TrackDbo> {
        return contentResolverHelper.getAudioData()
    }

}