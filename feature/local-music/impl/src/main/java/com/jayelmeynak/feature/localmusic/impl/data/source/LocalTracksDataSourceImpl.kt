package com.jayelmeynak.feature.localmusic.impl.data.source

import com.jayelmeynak.lib.mediastore.data.ContentResolverHelper
import com.jayelmeynak.lib.mediastore.data.TrackDbo
import com.jayelmeynak.util.coroutines.Dispatcher
import com.jayelmeynak.util.coroutines.MusicPlayerDispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class LocalTracksDataSourceImpl @Inject constructor(
    private val contentResolverHelper: ContentResolverHelper,
    @Dispatcher(MusicPlayerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : LocalTracksDataSource {

    override suspend fun getTracksList(): List<TrackDbo> = withContext(ioDispatcher) {
        contentResolverHelper.getAudioData()
    }
}
