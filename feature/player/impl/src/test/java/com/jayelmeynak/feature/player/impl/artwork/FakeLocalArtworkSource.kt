package com.jayelmeynak.feature.player.impl.artwork

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred

internal class FakeLocalArtworkSource(
    var artworks: Map<String, ByteArray?> = emptyMap(),
) : LocalArtworkSource {

    /** Requests for these track ids suspend until the test completes the deferred. */
    val pending = mutableMapOf<String, CompletableDeferred<ByteArray?>>()

    val requested = mutableListOf<String>()

    /** Ids whose request was cancelled while suspended. */
    val cancelled = mutableListOf<String>()

    override suspend fun artwork(trackId: String): ByteArray? {
        requested += trackId
        val waiting = pending[trackId] ?: return artworks[trackId]
        try {
            return waiting.await()
        } catch (e: CancellationException) {
            cancelled += trackId
            throw e
        }
    }
}
