package com.jayelmeynak.feature.localmusic.impl.presentation

import com.jayelmeynak.lib.mediastore.domain.repository.ArtworkRepository
import kotlinx.coroutines.CompletableDeferred

internal class FakeArtworkRepository(
    var artworks: Map<Long, ByteArray?> = emptyMap(),
) : ArtworkRepository {

    val prunedIds = mutableListOf<List<Long>>()

    /** Запрос обложки этих id бросает исключение. */
    var failingArtworks: Set<Long> = emptySet()

    /** Запрос обложки этих id ждёт, пока тест не завершит deferred. */
    val pendingArtworks = mutableMapOf<Long, CompletableDeferred<ByteArray?>>()

    override suspend fun getArtwork(trackId: Long, uri: String): ByteArray? {
        if (trackId in failingArtworks) throw IllegalStateException("broken artwork $trackId")
        return pendingArtworks[trackId]?.await() ?: artworks[trackId]
    }

    override suspend fun pruneArtworkCache(activeTrackIds: List<Long>) {
        prunedIds += activeTrackIds
    }
}
