package com.jayelmeynak.musicplayer.presentation.navigation

import com.jayelmeynak.feature.player.api.QueueItem
import com.jayelmeynak.feature.player.api.TrackSource
import com.jayelmeynak.lib.mediastore.domain.model.LocalTrack

/**
 * A device track as a queue item. Its artwork URI is the track's own `content://` URI: the player
 * reads the cover for it from the artwork cache.
 */
internal fun LocalTrack.toQueueItem(): QueueItem = QueueItem(
    id = uri.toString(),
    source = TrackSource.LOCAL,
    title = title,
    artist = artistName,
    artworkUri = uri.toString(),
    durationMs = duration.toLong(),
)
