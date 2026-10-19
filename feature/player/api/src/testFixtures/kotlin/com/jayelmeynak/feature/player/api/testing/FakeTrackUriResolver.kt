package com.jayelmeynak.feature.player.api.testing

import com.jayelmeynak.feature.player.api.TrackSource
import com.jayelmeynak.feature.player.api.TrackUriResolver

/**
 * [TrackUriResolver] для тестов: обслуживает [source], возвращает `uris[id]` (`null` для
 * неизвестных id) и записывает каждый запрошенный id в [requested].
 */
public class FakeTrackUriResolver(
    override val source: TrackSource,
    private val uris: Map<String, String?> = emptyMap(),
) : TrackUriResolver {

    private val recorded = mutableListOf<String>()

    /** Id, переданные в [resolve], в порядке вызова. */
    public val requested: List<String> get() = recorded.toList()

    override suspend fun resolve(id: String): String? {
        recorded += id
        return uris[id]
    }
}
