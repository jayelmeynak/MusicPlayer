package com.jayelmeynak.feature.player.impl.persistence

import androidx.media3.common.Player
import androidx.media3.common.Timeline
import com.jayelmeynak.feature.player.api.key
import com.jayelmeynak.feature.player.impl.service.toQueueItem
import com.jayelmeynak.util.coroutines.ApplicationScope
import com.jayelmeynak.util.coroutines.Dispatcher
import com.jayelmeynak.util.coroutines.MusicPlayerDispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Пауза между последней сменой очереди и её записью: серия тапов даёт одну запись. */
internal const val QUEUE_SAVE_DEBOUNCE_MS = 1_000L

/** Как часто позиция пишется во время игры: столько можно потерять при смерти процесса. */
internal const val POSITION_SAVE_PERIOD_MS = 10_000L

/**
 * Сохраняет очередь и позицию плеера сессии для возобновления: очередь — на смену плейлиста
 * (`TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED`; обновление источника при подготовке каждого трека
 * очередь не меняет) с дебаунсом и окном не больше [MAX_SAVED_QUEUE_SIZE], позиция — на
 * паузу/старт, переход, перемотку, раз в [POSITION_SAVE_PERIOD_MS] во время игры и в [release].
 * Текущий элемент ушёл за окно (переходы подряд по длинной очереди) — окно пересчитывается вокруг
 * него и очередь пишется сразу, иначе сохранённый индекс потерялся бы.
 *
 * Восстановленная сессия (`SessionRestorer`) тоже меняет плейлист: очередь перезаписывается тем же
 * содержимым один раз — безвредно, а окно получает актуальную привязку к плееру.
 *
 * Значения снимаются с плеера сразу на главном потоке, запись идёт в скоупе приложения: она
 * переживает уничтожение сервиса. Пустая очередь не пишется — сохранённая остаётся.
 */
internal class PlaybackPersistence @Inject constructor(
    private val store: PlaybackStateStore,
    @ApplicationScope private val scope: CoroutineScope,
    @Dispatcher(MusicPlayerDispatchers.Main) private val mainDispatcher: CoroutineDispatcher,
) {

    private var player: Player? = null
    private var window: QueueWindow? = null
    private var pendingQueue: QueueWindow? = null
    private var queueJob: Job? = null
    private var periodicJob: Job? = null
    private var playlistChanged = false

    private val listener = object : Player.Listener {
        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            if (reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) playlistChanged = true
        }

        // Media3 зовёт onEvents после отдельных колбэков того же пакета событий.
        override fun onEvents(player: Player, events: Player.Events) {
            if (playlistChanged) {
                playlistChanged = false
                onQueueChanged(player)
            }
            if (events.containsAny(
                    Player.EVENT_IS_PLAYING_CHANGED,
                    Player.EVENT_MEDIA_ITEM_TRANSITION,
                    Player.EVENT_POSITION_DISCONTINUITY,
                )
            ) {
                savePosition(player)
            }
            if (events.contains(Player.EVENT_IS_PLAYING_CHANGED)) updatePeriodicSave(player)
        }
    }

    fun attach(player: Player) {
        check(this.player == null) { "Сохранение уже подключено к плееру" }
        this.player = player
        player.addListener(listener)
    }

    /** Дописывает отложенную очередь и позицию, останавливает запись; плеер ещё не освобождён. */
    fun release() {
        val player = player ?: return
        player.removeListener(listener)
        periodicJob?.cancel()
        periodicJob = null
        if (queueJob?.isActive == true) {
            queueJob?.cancel()
            pendingQueue?.let(::writeQueue)
        }
        queueJob = null
        savePosition(player)
        this.player = null
    }

    private fun onQueueChanged(player: Player) {
        val next = snapshotWindow(player) ?: return
        window = next
        pendingQueue = next
        queueJob?.cancel()
        queueJob = scope.launch(mainDispatcher) {
            delay(QUEUE_SAVE_DEBOUNCE_MS)
            writeQueue(next)
            this@PlaybackPersistence.player?.let(::savePosition)
        }
    }

    private fun snapshotWindow(player: Player): QueueWindow? {
        val queue = List(player.mediaItemCount) { player.getMediaItemAt(it).toQueueItem() }
        if (queue.none { it != null }) return null
        return queueWindow(queue, player.currentMediaItemIndex)
    }

    private fun writeQueue(queue: QueueWindow) {
        pendingQueue = null
        val saved = SavedQueue(queue.items.map { it.toSaved() })
        scope.launch(mainDispatcher) { store.saveQueue(saved) }
    }

    private fun savePosition(player: Player) {
        val key = player.currentMediaItem?.toQueueItem()?.key ?: return
        // По индексу плеера, не по ключу: повторяющийся трек сохраняется тем экземпляром, что играет.
        var index = window?.playerIndices?.indexOf(player.currentMediaItemIndex) ?: -1
        if (index < 0 && queueJob?.isActive != true) {
            // Окно отстало от плеера: пересчитать и записать сразу, отложенной записи нет.
            snapshotWindow(player)?.let { next ->
                window = next
                writeQueue(next)
                index = next.currentIndex
            }
        }
        val position = SavedPosition(index, key, player.currentPosition.coerceAtLeast(0L))
        scope.launch(mainDispatcher) { store.savePosition(position) }
    }

    private fun updatePeriodicSave(player: Player) {
        if (!player.isPlaying) {
            periodicJob?.cancel()
            periodicJob = null
            return
        }
        if (periodicJob?.isActive == true) return
        periodicJob = scope.launch(mainDispatcher) {
            while (isActive) {
                delay(POSITION_SAVE_PERIOD_MS)
                savePosition(player)
            }
        }
    }
}
