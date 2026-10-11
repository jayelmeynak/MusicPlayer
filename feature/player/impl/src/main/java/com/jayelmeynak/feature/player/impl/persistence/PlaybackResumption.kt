package com.jayelmeynak.feature.player.impl.persistence

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import com.jayelmeynak.feature.player.impl.service.toMediaItem
import com.jayelmeynak.feature.player.impl.service.withTrackUri
import javax.inject.Inject

/**
 * Сохранённое воспроизведение как вход для плеера сессии: элементы уже с внутренними URI (Media3
 * ставит результат `onPlaybackResumption` в плеер напрямую, минуя `onAddMediaItems`), играбельную
 * ссылку даст резолвер источника перед загрузкой — для Deezer свежую.
 */
@OptIn(UnstableApi::class)
internal class PlaybackResumption @Inject constructor(
    private val store: PlaybackStateStore,
) {

    /**
     * [forPlayback] — вся очередь, индекс и позиция; иначе только текущий элемент (метаданные для
     * системы). `null` — возобновлять нечего.
     */
    suspend fun load(forPlayback: Boolean): MediaItemsWithStartPosition? {
        val point = restorePoint(store.readQueue(), store.readPosition()) ?: return null
        val items = if (forPlayback) point.items else listOf(point.items[point.index])
        return MediaItemsWithStartPosition(
            items.map { it.toMediaItem().withTrackUri() },
            if (forPlayback) point.index else 0,
            point.positionMs,
        )
    }
}
