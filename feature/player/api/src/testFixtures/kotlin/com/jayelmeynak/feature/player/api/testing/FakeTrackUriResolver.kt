package com.jayelmeynak.feature.player.api.testing

import com.jayelmeynak.feature.player.api.TrackUriResolver

/**
 * [TrackUriResolver] for tests: returns `uris[id]` (`null` for unknown ids) and records every
 * requested id in [requested].
 */
public class FakeTrackUriResolver(
    private val uris: Map<String, String?> = emptyMap(),
) : TrackUriResolver {

    private val recorded = mutableListOf<String>()

    /** Ids passed to [resolve], in call order. */
    public val requested: List<String> get() = recorded.toList()

    override suspend fun resolve(id: String): String? {
        recorded += id
        return uris[id]
    }
}
