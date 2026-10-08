package com.jayelmeynak.musicplayer.presentation.navigation

import com.jayelmeynak.feature.player.api.QueueItem
import com.jayelmeynak.feature.player.api.TrackSource
import com.jayelmeynak.lib.mediastore.domain.model.LocalTrack
import com.jayelmeynak.search_tracks.domain.models.Track

/** A Deezer track as a queue item: id and metadata only, never the expiring preview URL. */
internal fun Track.toQueueItem(): QueueItem = QueueItem(
    id = id.toString(),
    source = TrackSource.DEEZER,
    title = title,
    artist = artistName,
    artworkUri = album.cover.ifEmpty { null },
    durationMs = duration,
)

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
