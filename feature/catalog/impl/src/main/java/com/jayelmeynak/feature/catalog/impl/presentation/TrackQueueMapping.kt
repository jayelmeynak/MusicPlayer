package com.jayelmeynak.feature.catalog.impl.presentation

import com.jayelmeynak.feature.catalog.impl.domain.model.Track
import com.jayelmeynak.feature.player.api.QueueItem
import com.jayelmeynak.feature.player.api.TrackSource

/** Трек Deezer как элемент очереди плеера (адаптер к `player:api`): id и метаданные, без истекающего URL превью. */
internal fun Track.toQueueItem(): QueueItem = QueueItem(
    id = id.toString(),
    source = TrackSource.DEEZER,
    title = title,
    artist = artistName,
    artworkUri = coverUrl.ifEmpty { null },
    durationMs = duration,
)
