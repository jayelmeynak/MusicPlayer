package com.jayelmeynak.feature.player.api.testing

import com.jayelmeynak.feature.player.api.PlaybackState
import com.jayelmeynak.feature.player.api.QueueItem
import com.jayelmeynak.feature.player.api.TrackSource
import com.jayelmeynak.feature.player.api.testing.FakePlaybackController.Command
import org.junit.Assert.assertEquals
import org.junit.Test

/** То, что есть только у фейка и на что опираются тесты экранов; общий контракт — [FakePlaybackControllerContractTest]. */
class FakePlaybackControllerTest {

    private val first = QueueItem("1", TrackSource.DEEZER, "One", "A", null, 30_000L)
    private val second = QueueItem("2", TrackSource.DEEZER, "Two", "B", null, 45_000L)

    @Test
    fun `commands - все вызовы по порядку, и игнорируемые без подключения тоже`() {
        val controller = FakePlaybackController(connected = false)

        controller.play(listOf(first), 0)
        controller.togglePlayPause()
        controller.seekTo(1_000L)
        controller.next()
        controller.previous()
        controller.seekBack()
        controller.seekForward()

        assertEquals(
            listOf(
                Command.Play(listOf(first), 0),
                Command.TogglePlayPause,
                Command.SeekTo(1_000L),
                Command.Next,
                Command.Previous,
                Command.SeekBack,
                Command.SeekForward,
            ),
            controller.commands,
        )
    }

    @Test
    fun `перемотка недоступна - seekTo записан, но позицию не меняет`() {
        val controller = FakePlaybackController()
        controller.play(listOf(first), 0)
        controller.seekTo(1_000L)
        controller.state.value = (controller.state.value as PlaybackState.Active).copy(canSeek = false)

        controller.seekTo(5_000L)

        assertEquals(Command.SeekTo(5_000L), controller.commands.last())
        assertEquals(1_000L, controller.positionMs.value)
    }

    @Test
    fun `failConnection - ConnectionFailed`() {
        val controller = FakePlaybackController(connected = false)

        controller.failConnection()

        assertEquals(PlaybackState.ConnectionFailed, controller.state.value)
    }

    @Test
    fun `disconnect без снапшота оставляет Idle`() {
        val controller = FakePlaybackController(connected = false)

        controller.disconnect()

        assertEquals(PlaybackState.Idle, controller.state.value)
    }

    @Test
    fun `play - длительность из элемента очереди`() {
        val controller = FakePlaybackController()

        controller.play(listOf(first, second), 1)

        assertEquals(45_000L, (controller.state.value as PlaybackState.Active).durationMs)
    }
}
