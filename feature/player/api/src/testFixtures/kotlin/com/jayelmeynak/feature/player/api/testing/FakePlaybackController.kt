package com.jayelmeynak.feature.player.api.testing

import com.jayelmeynak.feature.player.api.PlaybackController
import com.jayelmeynak.feature.player.api.PlaybackState
import com.jayelmeynak.feature.player.api.QueueItem
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * [PlaybackController] for tests: records every call in [commands], including calls the contract
 * ignores while idle, so a test can check what was sent.
 *
 * A [connected] fake starts as an empty [PlaybackState.Active]; one created with
 * `connected = false` starts as [PlaybackState.Idle], defers [play] (the last call wins) and
 * applies it on [connect]. Only [play] and [connect] change [state] and [positionMs]; for any
 * other transition the test sets them itself. Transport commands never change the state.
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

    /** Connects to the session: plays the deferred queue, if any, or becomes an empty `Active`. */
    public fun connect() {
        isConnected = true
        val pending = deferredPlay
        deferredPlay = null
        if (pending != null) start(pending) else state.value = emptyActive()
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
