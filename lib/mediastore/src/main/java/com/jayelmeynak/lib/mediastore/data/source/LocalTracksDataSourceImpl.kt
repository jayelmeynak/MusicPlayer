package com.jayelmeynak.lib.mediastore.data.source

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.jayelmeynak.lib.mediastore.data.ContentResolverHelper
import com.jayelmeynak.lib.mediastore.data.TrackDbo
import com.jayelmeynak.lib.database.data.db.ArtworkDao
import com.jayelmeynak.lib.database.data.db.ArtworkEntity
import com.jayelmeynak.util.coroutines.Dispatcher
import com.jayelmeynak.util.coroutines.MusicPlayerDispatchers
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class LocalTracksDataSourceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contentResolverHelper: ContentResolverHelper,
    private val artworkDao: ArtworkDao,
    @Dispatcher(MusicPlayerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : LocalTracksDataSource {

    override suspend fun getTracksList(): List<TrackDbo> = withContext(ioDispatcher) {
        contentResolverHelper.getAudioData()
    }

    override suspend fun getArtwork(trackId: Long, uri: Uri): ByteArray? =
        withContext(ioDispatcher) {
            artworkDao.getByTrackId(trackId)?.let { cached ->
                return@withContext cached.data
            }

            val artwork = runCatching {
                MediaMetadataRetriever().use { retriever ->
                    retriever.setDataSource(context, uri)
                    retriever.embeddedPicture
                }
            }.getOrNull()

            artworkDao.insert(ArtworkEntity(trackId = trackId, data = artwork))
            artwork
        }

    override suspend fun pruneArtworkCache(activeTrackIds: List<Long>) =
        withContext(ioDispatcher) {
            artworkDao.deleteStale(activeTrackIds)
        }
}
