package com.jayelmeynak.feature.player.impl.service

import android.net.Uri
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.jayelmeynak.feature.player.api.QueueItem
import com.jayelmeynak.feature.player.api.TrackSource

/**
 * MediaMetadata extras key for the full track length in ms. Not durationMs: for Deezer the media is
 * a 30-second preview, and Media3 shows durationMs in system controls until the player knows better.
 */
internal const val EXTRA_TRACK_DURATION_MS = "com.jayelmeynak.player.TRACK_DURATION_MS"

/** Scheme of the URI the session gives a queue item; only the player's data source reads it. */
internal const val TRACK_URI_SCHEME = "musicplayer-track"

private const val MEDIA_ID_SEPARATOR = '|'

/** What identifies a track for [com.jayelmeynak.feature.player.api.TrackUriResolver]. */
internal data class TrackKey(val source: TrackSource, val id: String)

/**
 * The queue item as a session item: `mediaId` = `SOURCE|id`, title, artist and artwork in the
 * metadata, the full length in extras. No URI: the session sets its own in `onAddMediaItems`.
 */
internal fun QueueItem.toMediaItem(): MediaItem = MediaItem.Builder()
    .setMediaId("${source.name}$MEDIA_ID_SEPARATOR$id")
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setArtworkUri(artworkUri?.toUri())
            .setExtras(bundleOf(EXTRA_TRACK_DURATION_MS to durationMs))
            .build()
    )
    .build()

/** The queue item back from a session item, or `null` if the item was not made by [toMediaItem]. */
internal fun MediaItem.toQueueItem(): QueueItem? {
    val key = trackKey() ?: return null
    return QueueItem(
        id = key.id,
        source = key.source,
        title = mediaMetadata.title?.toString().orEmpty(),
        artist = mediaMetadata.artist?.toString().orEmpty(),
        artworkUri = mediaMetadata.artworkUri?.toString(),
        durationMs = mediaMetadata.extras?.getLong(EXTRA_TRACK_DURATION_MS) ?: 0L,
    )
}

/** The track behind a session item, read from its `mediaId`; `null` for a foreign item. */
internal fun MediaItem.trackKey(): TrackKey? {
    val separator = mediaId.indexOf(MEDIA_ID_SEPARATOR)
    if (separator <= 0) return null
    val source = trackSourceOf(mediaId.substring(0, separator)) ?: return null
    val id = mediaId.substring(separator + 1).takeIf { it.isNotEmpty() } ?: return null
    return TrackKey(source, id)
}

/** `musicplayer-track://SOURCE/<encoded id>`: never leaves the player. */
internal fun TrackKey.toTrackUri(): Uri = Uri.Builder()
    .scheme(TRACK_URI_SCHEME)
    .authority(source.name)
    .appendPath(id)
    .build()

/** The track behind a URI made by [toTrackUri], or `null` for any other URI. */
internal fun Uri.toTrackKey(): TrackKey? {
    if (scheme != TRACK_URI_SCHEME) return null
    val source = authority?.let(::trackSourceOf) ?: return null
    val id = pathSegments.singleOrNull()?.takeIf { it.isNotEmpty() } ?: return null
    return TrackKey(source, id)
}

private fun trackSourceOf(name: String): TrackSource? = TrackSource.entries.firstOrNull { it.name == name }
