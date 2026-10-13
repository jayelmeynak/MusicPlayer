package com.jayelmeynak.lib.mediastore.data

import android.content.Context
import android.media.MediaMetadataRetriever
import androidx.core.net.toUri
import com.jayelmeynak.lib.database.data.db.ArtworkDao
import com.jayelmeynak.lib.database.data.db.ArtworkEntity
import com.jayelmeynak.lib.mediastore.domain.repository.ArtworkRepository
import com.jayelmeynak.util.coroutines.Dispatcher
import com.jayelmeynak.util.coroutines.MusicPlayerDispatchers
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class ArtworkRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val artworkDao: ArtworkDao,
    @Dispatcher(MusicPlayerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : ArtworkRepository {

    override suspend fun getArtwork(trackId: Long, uri: String): ByteArray? =
        withContext(ioDispatcher) {
            artworkDao.getByTrackId(trackId)?.let { cached ->
                return@withContext cached.data
            }

            val artwork = runCatching {
                MediaMetadataRetriever().use { retriever ->
                    retriever.setDataSource(context, uri.toUri())
                    retriever.embeddedPicture
                }
            }.getOrNull()

            artworkDao.insert(ArtworkEntity(trackId = trackId, data = artwork))
            artwork
        }

    override suspend fun pruneArtworkCache(activeTrackIds: List<Long>): Unit =
        withContext(ioDispatcher) {
            artworkDao.deleteStale(activeTrackIds)
        }
}
