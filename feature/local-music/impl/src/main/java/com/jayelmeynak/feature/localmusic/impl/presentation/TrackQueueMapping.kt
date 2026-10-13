package com.jayelmeynak.feature.localmusic.impl.presentation

import com.jayelmeynak.feature.localmusic.impl.domain.model.LocalTrack
import com.jayelmeynak.feature.player.api.QueueItem
import com.jayelmeynak.feature.player.api.TrackSource

/**
 * Трек устройства как элемент очереди. URI обложки — собственный `content://` URI трека: обложка
 * трека — эскиз провайдера (`ContentResolver.loadThumbnail`): так его читают список и плеер.
 */
internal fun LocalTrack.toQueueItem(): QueueItem = QueueItem(
    id = uri,
    source = TrackSource.LOCAL,
    title = title,
    artist = artistName,
    artworkUri = uri,
    durationMs = duration.toLong(),
)
