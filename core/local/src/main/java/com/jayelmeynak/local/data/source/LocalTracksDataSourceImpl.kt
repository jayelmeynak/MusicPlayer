package com.jayelmeynak.local.data.source

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.jayelmeynak.local.data.ContentResolverHelper
import com.jayelmeynak.local.data.TrackDbo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class LocalTracksDataSourceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contentResolverHelper: ContentResolverHelper
) : LocalTracksDataSource {

    override suspend fun getTracksList(): List<TrackDbo> = withContext(Dispatchers.IO) {
        contentResolverHelper.getAudioData()
    }

    override suspend fun getArtwork(uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
        runCatching {
            MediaMetadataRetriever().use { retriever ->
                retriever.setDataSource(context, uri)
                retriever.embeddedPicture
            }
        }.getOrNull()
    }
}