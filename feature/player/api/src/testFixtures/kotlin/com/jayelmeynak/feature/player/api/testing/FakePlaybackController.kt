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
 * [failConnection] даёт [PlaybackState.ConnectionFailed]. [state] и [positionMs] меняют только
 * [play], [connect], [disconnect] и [failConnection]; любой другой переход тест задаёт сам.
 * Транспортные команды состояние не меняют.
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

    /** Calls in call order. */
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
        if (isConnected) start(command) else deferredPlay = command
    }

    override fun togglePlayPause() {
        recorded += Command.TogglePlayPause
    }

    override fun seekTo(positionMs: Long) {
        recorded += Command.SeekTo(positionMs)
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

    private fun start(command: Command.Play) {
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
        )
    }

    /** A call to one of the [PlaybackController] commands. */
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
        )
    }
}
