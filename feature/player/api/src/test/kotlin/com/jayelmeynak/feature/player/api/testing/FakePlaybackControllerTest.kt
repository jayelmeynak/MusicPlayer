package com.jayelmeynak.feature.player.api.testing

import com.jayelmeynak.feature.player.api.PlaybackState
import com.jayelmeynak.feature.player.api.QueueItem
import com.jayelmeynak.feature.player.api.TrackSource
import com.jayelmeynak.feature.player.api.testing.FakePlaybackController.Command
import org.junit.Assert.assertEquals
import org.junit.Test

class FakePlaybackControllerTest {

    private val first = item("1", durationMs = 30_000)
    private val second = item("2", durationMs = 45_000)

    @Test
    fun `connected controller is active with an empty queue`() {
        val controller = FakePlaybackController()

        assertEquals(emptyActive(), controller.state.value)
        assertEquals(0L, controller.positionMs.value)
    }

    @Test
    fun `play on a connected controller makes the state active with the queue and the start index`() {
        val controller = FakePlaybackController()

        controller.play(listOf(first, second), startIndex = 1)

        assertEquals(playing(listOf(first, second), currentIndex = 1, durationMs = 45_000), controller.state.value)
    }

    @Test
    fun `play resets the position`() {
        val controller = FakePlaybackController()
        controller.play(listOf(first), startIndex = 0)
        controller.positionMs.value = 12_000

        controller.play(listOf(second), startIndex = 0)

        assertEquals(0L, controller.positionMs.value)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `play rejects an empty queue`() {
        FakePlaybackController().play(emptyList(), startIndex = 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `play rejects an index past the queue`() {
        FakePlaybackController().play(listOf(first), startIndex = 1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `play rejects a negative index`() {
        FakePlaybackController().play(listOf(first), startIndex = -1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `idle controller also rejects an invalid play`() {
        FakePlaybackController(connected = false).play(emptyList(), startIndex = 0)
    }

    @Test
    fun `not connected controller stays idle on play and records the command`() {
        val controller = FakePlaybackController(connected = false)

        controller.play(listOf(first), startIndex = 0)

        assertEquals(PlaybackState.Idle, controller.state.value)
        assertEquals(listOf(Command.Play(listOf(first), startIndex = 0)), controller.commands)
    }

    @Test
    fun `connect plays the queue of the last deferred play`() {
        val controller = FakePlaybackController(connected = false)
        controller.play(listOf(first), startIndex = 0)
        controller.play(listOf(first, second), startIndex = 1)
        controller.positionMs.value = 5_000

        controller.connect()

        assertEquals(playing(listOf(first, second), currentIndex = 1, durationMs = 45_000), controller.state.value)
        assertEquals(0L, controller.positionMs.value)
    }

    @Test
    fun `connect without a deferred play gives an empty active state`() {
        val controller = FakePlaybackController(connected = false)

        controller.connect()

        assertEquals(emptyActive(), controller.state.value)
    }

    @Test
    fun `transport commands while idle are recorded but do not change the state`() {
        val controller = FakePlaybackController(connected = false)

        controller.togglePlayPause()
        controller.seekTo(1_000)
        controller.next()

        assertEquals(PlaybackState.Idle, controller.state.value)
        assertEquals(0L, controller.positionMs.value)
        assertEquals(listOf(Command.TogglePlayPause, Command.SeekTo(1_000), Command.Next), controller.commands)
    }

    @Test
    fun `commands are recorded in call order`() {
        val controller = FakePlaybackController()

        controller.play(listOf(first), startIndex = 0)
        controller.togglePlayPause()
        controller.seekTo(1_000)
        controller.next()
        controller.previous()
        controller.seekBack()
        controller.seekForward()

        assertEquals(
            listOf(
                Command.Play(listOf(first), startIndex = 0),
                Command.TogglePlayPause,
                Command.SeekTo(1_000),
                Command.Next,
                Command.Previous,
                Command.SeekBack,
                Command.SeekForward,
            ),
            controller.commands,
        )
    }

    @Test
    fun `transport commands leave the state to the test`() {
        val controller = FakePlaybackController()
        controller.play(listOf(first, second), startIndex = 0)
        val afterPlay = controller.state.value

        controller.next()
        controller.togglePlayPause()

        assertEquals(afterPlay, controller.state.value)
    }

    private fun playing(queue: List<QueueItem>, currentIndex: Int, durationMs: Long) = PlaybackState.Active(
        queue = queue,
        currentIndex = currentIndex,
        isPlaying = true,
        playWhenReady = true,
        isBuffering = false,
        durationMs = durationMs,
        error = null,
    )

    private fun emptyActive() = PlaybackState.Active(
        queue = emptyList(),
        currentIndex = 0,
        isPlaying = false,
        playWhenReady = false,
        isBuffering = false,
        durationMs = 0,
        error = null,
    )

    private fun item(id: String, durationMs: Long) = QueueItem(
        id = id,
        source = TrackSource.DEEZER,
        title = "Title $id",
        artist = "Artist",
        artworkUri = null,
        durationMs = durationMs,
    )
}
