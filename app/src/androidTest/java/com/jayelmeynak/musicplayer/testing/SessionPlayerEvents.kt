package com.jayelmeynak.musicplayer.testing

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.TimeUnit

/**
 * Отдельный контроллер теста на сессии сервиса плеера: записывает события плеера сессии, по которым
 * виден перезапуск текущего элемента — замена очереди, смена элемента, скачок позиции, уход в
 * буферизацию или IDLE. Тест работает в процессе приложения — сессия принимает контроллер своего uid.
 *
 * Слушатель вызывается на главном потоке; методы класса читают записи и плеер на нём же.
 */
class SessionPlayerEvents private constructor(private val controller: MediaController) : AutoCloseable {

    private val events = mutableListOf<String>()

    private val listener = object : Player.Listener {
        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            if (reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) events += "очередь заменена"
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            events += "смена элемента на ${mediaItem?.mediaId}, причина $reason"
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int,
        ) {
            events += "скачок позиции ${oldPosition.positionMs} → ${newPosition.positionMs}, причина $reason"
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_IDLE) {
                events += "состояние плеера $playbackState"
            }
        }
    }

    init {
        onMain { controller.addListener(listener) }
    }

    /** Признаки перезапуска с момента подключения. */
    fun restarts(): List<String> {
        var snapshot: List<String> = emptyList()
        onMain { snapshot = events.toList() }
        return snapshot
    }

    /** `mediaId` текущего элемента сессии. */
    fun currentMediaId(): String? {
        var mediaId: String? = null
        onMain { mediaId = controller.currentMediaItem?.mediaId }
        return mediaId
    }

    override fun close() {
        onMain { controller.release() }
    }

    companion object {

        /** Подключается к сессии сервиса своего пакета; ждёт подключения не дольше [timeoutMs]. */
        fun connect(context: Context, timeoutMs: Long): SessionPlayerEvents {
            val token = SessionToken.getAllServiceTokens(context).single { it.packageName == context.packageName }
            var future: ListenableFuture<MediaController>? = null
            // Контроллер создаётся на потоке с Looper — главном, как и у приложения.
            onMain { future = MediaController.Builder(context, token).buildAsync() }
            val pending = checkNotNull(future)
            val controller = try {
                pending.get(timeoutMs, TimeUnit.MILLISECONDS)
            } catch (e: Exception) {
                // Опоздавший контроллер иначе остался бы привязан к сессии до конца прогона.
                onMain { MediaController.releaseFuture(pending) }
                throw e
            }
            return SessionPlayerEvents(controller)
        }

        private fun onMain(block: () -> Unit) = InstrumentationRegistry.getInstrumentation().runOnMainSync(block)
    }
}
