package com.jayelmeynak.feature.player.api.testing

import com.jayelmeynak.feature.player.api.PlaybackController
import com.jayelmeynak.feature.player.api.PlaybackState
import com.jayelmeynak.feature.player.api.QueueItem
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * [PlaybackController] для тестов: записывает каждый вызов в [commands], в том числе те, что
 * контракт игнорирует без подключения, — тест проверяет, что было отправлено.
 *
 * Фейк с [connected] стартует пустым [PlaybackState.Active]; созданный с `connected = false` — в
 * [PlaybackState.Idle], откладывает [play] (выигрывает последний вызов) и применяет его в [connect].
 * [disconnect] оставляет последний снапшот с `isConnected = false`, как настоящий контроллер в фоне;
 * [failConnection] даёт [PlaybackState.ConnectionFailed]. Как настоящий контроллер (общий контракт —
 * [PlaybackControllerContract]): [play] сразу обнуляет [positionMs], даже до подключения; [play]
 * текущей очереди и трека не перезапускает трек и не трогает позицию, а только возобновляет;
 * [seekTo] при подключении сразу пишет позицию, если снапшот разрешает перемотку
 * ([PlaybackState.Active.canSeek]). Пустой снапшот перемотку запрещает, трек из [play] сразу
 * перематывается (подготовки у фейка нет); трек без перемотки тест задаёт сам через [state].
 * Остальные транспортные команды состояние не меняют —
 * любой другой переход тест задаёт сам. Расхождение: [play] того же трека в фоне оставляет позицию 0
 * и после [connect], а настоящий контроллер после подключения берёт позицию сессии.
 */
public class FakePlaybackController(
    connected: Boolean = true,
) : PlaybackController {

    override val state: MutableStateFlow<PlaybackState> =
        MutableStateFlow(if (connected) emptyActive() else PlaybackState.Idle)

    override val positionMs: MutableStateFlow<Long> = MutableStateFlow(0L)

    private var isConnected = connected

    private var deferredPlay: Command.Play? = null

    private val recorded = mutableListOf<Command>()

    /** Вызовы в порядке вызова. */
    public val commands: List<Command> get() = recorded.toList()

    /**
     * Подключение к сессии: играет отложенную очередь, если она есть; иначе последний снапшот снова
     * подключён, а без снапшота — пустой `Active`.
     */
    public fun connect() {
        isConnected = true
        val pending = deferredPlay
        deferredPlay = null
        val last = state.value as? PlaybackState.Active
        when {
            pending != null -> start(pending)
            last != null -> state.value = last.copy(isConnected = true)
            else -> state.value = emptyActive()
        }
    }

    /** Уход в фон: последний снапшот остаётся с `isConnected = false`; без снапшота — [PlaybackState.Idle]. */
    public fun disconnect() {
        isConnected = false
        val last = state.value as? PlaybackState.Active
        state.value = last?.copy(isConnected = false) ?: PlaybackState.Idle
    }

    /** Все попытки подключения не удались. */
    public fun failConnection() {
        isConnected = false
        state.value = PlaybackState.ConnectionFailed
    }

    override fun play(queue: List<QueueItem>, startIndex: Int) {
        require(startIndex in queue.indices) { "startIndex $startIndex is not in the queue of ${queue.size}" }
        val command = Command.Play(queue, startIndex)
        recorded += command
        if (isConnected) {
            start(command)
        } else {
            // Как у настоящего контроллера: тап сразу показывает начало трека, не дожидаясь подключения.
            positionMs.value = 0L
            deferredPlay = command
        }
    }

    override fun togglePlayPause() {
        recorded += Command.TogglePlayPause
    }

    override fun seekTo(positionMs: Long) {
        recorded += Command.SeekTo(positionMs)
        val canSeek = (state.value as? PlaybackState.Active)?.canSeek == true
        if (isConnected && canSeek) this.positionMs.value = positionMs
    }

    override fun next() {
        recorded += Command.Next
    }

    override fun previous() {
        recorded += Command.Previous
    }

    override fun seekBack() {
        recorded += Command.SeekBack
    }

    override fun seekForward() {
        recorded += Command.SeekForward
    }

    private fun isCurrent(command: Command.Play): Boolean {
        val current = state.value as? PlaybackState.Active ?: return false
        return current.queue == command.queue && current.currentIndex == command.startIndex
    }

    private fun start(command: Command.Play) {
        val current = state.value as? PlaybackState.Active
        if (current != null && isCurrent(command)) {
            // Тот же трек той же очереди: возобновление без перезапуска, позиция прежняя.
            state.value = current.copy(
                isPlaying = true,
                playWhenReady = true,
                isBuffering = false,
                showPlayButton = false,
                isConnected = true,
            )
            return
        }
        positionMs.value = 0L
        state.value = PlaybackState.Active(
            queue = command.queue,
            currentIndex = command.startIndex,
            isPlaying = true,
            playWhenReady = true,
            isBuffering = false,
            showPlayButton = false,
            durationMs = command.queue[command.startIndex].durationMs,
            error = null,
            canSeek = true,
            canSeekBack = true,
            canSeekForward = true,
        )
    }

    /** Вызов одной из команд [PlaybackController]. */
    public sealed interface Command {
        public data class Play(val queue: List<QueueItem>, val startIndex: Int) : Command
        public data object TogglePlayPause : Command
        public data class SeekTo(val positionMs: Long) : Command
        public data object Next : Command
        public data object Previous : Command
        public data object SeekBack : Command
        public data object SeekForward : Command
    }

    private companion object {
        fun emptyActive() = PlaybackState.Active(
            queue = emptyList(),
            currentIndex = 0,
            isPlaying = false,
            playWhenReady = false,
            isBuffering = false,
            showPlayButton = true,
            durationMs = 0L,
            error = null,
            canSeek = false,
            canSeekBack = false,
            canSeekForward = false,
        )
    }
}
