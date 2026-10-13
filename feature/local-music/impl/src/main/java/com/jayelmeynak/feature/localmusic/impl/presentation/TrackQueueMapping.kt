package com.jayelmeynak.feature.localmusic.impl.presentation

import com.jayelmeynak.feature.localmusic.impl.domain.model.LocalTrack
import com.jayelmeynak.feature.player.api.QueueItem
import com.jayelmeynak.feature.player.api.TrackSource

/**
 * Трек устройства как элемент очереди. URI обложки — собственный `content://` URI трека: обложку
 * для него плеер читает из кэша обложек.
 */
internal fun LocalTrack.toQueueItem(): QueueItem = QueueItem(
    id = uri,
    source = TrackSource.LOCAL,
    title = title,
    artist = artistName,
    artworkUri = uri,
    durationMs = duration.toLong(),
)
