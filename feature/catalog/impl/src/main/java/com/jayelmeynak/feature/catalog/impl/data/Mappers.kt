package com.jayelmeynak.feature.catalog.impl.data

import com.jayelmeynak.feature.catalog.impl.domain.model.Track
import com.jayelmeynak.lib.network.data.dto.TrackDto

/** Трек без `id`, `title` или `preview` — `null`: его нельзя показать и сыграть; сам URL превью не сохраняется, длительность Deezer (секунды) — в мс. */
internal fun TrackDto.toTrack(): Track? {
    if (preview == null) return null
    return Track(
        id = id ?: return null,
        title = title ?: return null,
        artistName = artist?.name.orEmpty(),
        coverUrl = album?.cover.orEmpty(),
        duration = duration?.times(1000L) ?: 0L,
    )
}
