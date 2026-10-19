package com.jayelmeynak.feature.player.impl.service

import android.net.Uri
import android.os.Bundle
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.jayelmeynak.feature.player.api.QueueItem
import com.jayelmeynak.feature.player.api.TrackSource
import com.jayelmeynak.feature.player.api.key

/**
 * Ключ extras в MediaMetadata для полной длины трека, мс. Не durationMs: у Deezer медиа — 30-секундное
 * превью, а Media3 показывает durationMs в системных контролах, пока плеер не узнает точнее.
 */
internal const val EXTRA_TRACK_DURATION_MS = "com.jayelmeynak.player.TRACK_DURATION_MS"

/** Схема URI, который сессия даёт элементу очереди; читает его только data source плеера. */
internal const val TRACK_URI_SCHEME = "musicplayer-track"

private const val MEDIA_ID_SEPARATOR = '|'

/** То, что идентифицирует трек для [com.jayelmeynak.feature.player.api.TrackUriResolver]. */
internal data class TrackKey(val source: TrackSource, val id: String)

/**
 * Элемент очереди как элемент сессии: `mediaId` = [QueueItem.key] (`SOURCE|id`), название, исполнитель и обложка —
 * в метаданных, полная длина — в extras. Без URI: сессия ставит свой в `onAddMediaItems`.
 */
internal fun QueueItem.toMediaItem(): MediaItem = MediaItem.Builder()
    .setMediaId(key)
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setArtworkUri(artworkUri?.toUri())
            .setExtras(Bundle().apply { putLong(EXTRA_TRACK_DURATION_MS, durationMs) })
            .build()
    )
    .build()

/** Элемент очереди обратно из элемента сессии или `null`, если элемент сделан не [toMediaItem]. */
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

/** Трек элемента сессии, прочитанный из его `mediaId`; `null` — для чужого элемента. */
internal fun MediaItem.trackKey(): TrackKey? {
    val separator = mediaId.indexOf(MEDIA_ID_SEPARATOR)
    if (separator <= 0) return null
    val source = trackSourceOf(mediaId.substring(0, separator)) ?: return null
    val id = mediaId.substring(separator + 1).takeIf { it.isNotEmpty() } ?: return null
    return TrackKey(source, id)
}

/** `musicplayer-track://SOURCE/<encoded id>`: никогда не покидает плеер. */
internal fun TrackKey.toTrackUri(): Uri = Uri.Builder()
    .scheme(TRACK_URI_SCHEME)
    .authority(source.name)
    .appendPath(id)
    .build()

/** Трек URI, сделанного [toTrackUri], или `null` для любого другого URI. */
internal fun Uri.toTrackKey(): TrackKey? {
    if (scheme != TRACK_URI_SCHEME) return null
    val source = authority?.let(::trackSourceOf) ?: return null
    val id = pathSegments.singleOrNull()?.takeIf { it.isNotEmpty() } ?: return null
    return TrackKey(source, id)
}

private fun trackSourceOf(name: String): TrackSource? = TrackSource.entries.firstOrNull { it.name == name }

/** Неразрешимый внутренний URI: элемент без ключа трека никогда не загрузится. */
internal const val UNRESOLVABLE_TRACK_URI = "$TRACK_URI_SCHEME://unknown"

/**
 * Элемент для плеера сессии: свой элемент получает внутренний URI своего трека, любой другой — URI,
 * который никогда не разрешится, даже если у него был собственный URI (см. `PlaybackSessionCallback`).
 */
internal fun MediaItem.withTrackUri(): MediaItem {
    val uri = trackKey()?.toTrackUri()?.toString() ?: UNRESOLVABLE_TRACK_URI
    return buildUpon().setUri(uri).build()
}
