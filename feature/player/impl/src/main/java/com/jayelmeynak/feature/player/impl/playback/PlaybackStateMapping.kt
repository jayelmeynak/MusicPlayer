package com.jayelmeynak.feature.player.impl.playback

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import com.jayelmeynak.feature.player.api.PlaybackError
import com.jayelmeynak.feature.player.api.PlaybackState
import com.jayelmeynak.feature.player.api.QueueItem
import com.jayelmeynak.feature.player.impl.service.toQueueItem

/** Элементы плеера как элементы очереди; `null` — для элемента, добавленного не этим приложением. */
internal fun Player.readQueue(): List<QueueItem?> =
    (0 until mediaItemCount).map { index -> getMediaItemAt(index).toQueueItem() }

/**
 * Всё состояние плеера разом, чтобы очередь, текущий индекс и флаги всегда сходились.
 * [queue] — результат [readQueue], передаётся снаружи, чтобы пересобирать его только при смене
 * timeline. Чужие элементы в очередь не попадают; если текущий элемент чужой,
 * [PlaybackState.Active.current] — `null`. Флаги перемотки — доступные команды плеера: у
 * `MediaController` они меняются событием `EVENT_AVAILABLE_COMMANDS_CHANGED`, на которое снапшот
 * пересобирается, как и на любое другое.
 */
@OptIn(UnstableApi::class)
internal fun Player.toPlaybackState(queue: List<QueueItem?> = readQueue()): PlaybackState.Active {
    val items = queue.filterNotNull()
    val index = currentMediaItemIndex
    val currentIndex = when {
        items.isEmpty() -> 0
        queue.getOrNull(index) == null -> -1
        else -> queue.subList(0, index).count { it != null }
    }
    return PlaybackState.Active(
        queue = items,
        currentIndex = currentIndex,
        isPlaying = isPlaying,
        playWhenReady = playWhenReady,
        isBuffering = playbackState == Player.STATE_BUFFERING,
        showPlayButton = Util.shouldShowPlayButton(this),
        durationMs = duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: 0L,
        error = playerError?.toPlaybackError(),
        canSeek = isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM),
        canSeekBack = isCommandAvailable(Player.COMMAND_SEEK_BACK),
        canSeekForward = isCommandAvailable(Player.COMMAND_SEEK_FORWARD),
    )
}

/**
 * Сеть (нет подключения, истекло ожидание соединения) — [PlaybackError.NETWORK]; прочие ошибки
 * ввода-вывода (резолвер не дал ссылку, ответ сервера, файл) — трек недоступен; остальное — неизвестная.
 */
internal fun PlaybackException.toPlaybackError(): PlaybackError = when {
    errorCode in NETWORK_ERROR_CODES -> PlaybackError.NETWORK
    errorCode / ERROR_GROUP == IO_ERROR_GROUP -> PlaybackError.SOURCE_UNAVAILABLE
    else -> PlaybackError.UNKNOWN
}

private val NETWORK_ERROR_CODES = setOf(
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
)

/** Media3 группирует коды ошибок по тысячам; 2xxx — ошибки ввода-вывода. */
private const val ERROR_GROUP = 1000
private const val IO_ERROR_GROUP = PlaybackException.ERROR_CODE_IO_UNSPECIFIED / ERROR_GROUP
