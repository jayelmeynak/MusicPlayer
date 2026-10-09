package com.jayelmeynak.feature.player.ui

import app.cash.turbine.test
import com.jayelmeynak.feature.player.api.PlaybackState
import com.jayelmeynak.feature.player.api.QueueItem
import com.jayelmeynak.feature.player.api.TrackSource
import com.jayelmeynak.feature.player.api.testing.FakePlaybackController
import com.jayelmeynak.feature.player.api.testing.FakePlaybackController.Command
import com.jayelmeynak.util.testing.MainDispatcherRule
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MiniPlayerViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    @Test
    fun `без подписчиков VM не собирает состояние и позицию контроллера`() = runTest {
        val controller = FakePlaybackController().apply { play(listOf(track(1)), 0) }
        val viewModel = MiniPlayerViewModel(controller)

        assertEquals(0, controller.state.subscriptionCount.value)
        assertEquals(0, controller.positionMs.subscriptionCount.value)

        viewModel.state.test {
            assertTrue(expectMostRecentItem().visible)
            assertEquals(1, controller.positionMs.subscriptionCount.value)
        }
    }

    @Test
    fun `контроллер не подключён - мини-плеер скрыт, после подключения с очередью показан`() = runTest {
        val controller = FakePlaybackController(connected = false)
        val viewModel = subscribed(MiniPlayerViewModel(controller))
        controller.play(listOf(track(1)), 0)

        assertFalse(viewModel.state.value.visible)

        controller.connect()

        assertTrue(viewModel.state.value.visible)
    }

    @Test
    fun `подключён с пустой очередью - мини-плеер скрыт`() = runTest {
        val viewModel = subscribed(MiniPlayerViewModel(FakePlaybackController()))

        assertFalse(viewModel.state.value.visible)
    }

    @Test
    fun `текущий трек - название, исполнитель и игра`() = runTest {
        val controller = FakePlaybackController().apply { play(listOf(track(1), track(2)), 1) }
        val viewModel = subscribed(MiniPlayerViewModel(controller))

        val state = viewModel.state.value
        assertEquals("Song 2", state.title)
        assertEquals("Artist 2", state.artist)
        assertFalse(state.showPlayButton)
        assertTrue(state.visible)
    }

    @Test
    fun `пауза в сессии - кнопка показывает воспроизведение`() = runTest {
        val controller = FakePlaybackController().apply { play(listOf(track(1)), 0) }
        val viewModel = subscribed(MiniPlayerViewModel(controller))

        controller.state.value = active(controller)
            .copy(isPlaying = false, playWhenReady = false, showPlayButton = true)

        assertTrue(viewModel.state.value.showPlayButton)
    }

    @Test
    fun `буферизация при запрошенном воспроизведении - кнопка паузы`() = runTest {
        val controller = FakePlaybackController().apply { play(listOf(track(1)), 0) }
        val viewModel = subscribed(MiniPlayerViewModel(controller))

        controller.state.value = active(controller)
            .copy(isPlaying = false, playWhenReady = true, isBuffering = true, showPlayButton = false)

        assertFalse(viewModel.state.value.showPlayButton)
        viewModel.onAction(MiniPlayerAction.TogglePlayPause)
        assertEquals(Command.TogglePlayPause, controller.commands.last())
    }

    @Test
    fun `прогресс - позиция в процентах длительности`() = runTest {
        val controller = FakePlaybackController().apply { play(listOf(track(1, durationMs = 200_000L)), 0) }
        val viewModel = subscribed(MiniPlayerViewModel(controller))

        controller.positionMs.value = 50_000L

        assertEquals(25f, viewModel.state.value.progress)
    }

    @Test
    fun `неизвестная длительность - прогресс 0`() = runTest {
        val controller = FakePlaybackController().apply { play(listOf(track(1, durationMs = 0L)), 0) }
        val viewModel = subscribed(MiniPlayerViewModel(controller))

        controller.positionMs.value = 50_000L

        assertEquals(0f, viewModel.state.value.progress)
    }

    @Test
    fun `смена трека меняет ключ трека`() = runTest {
        val controller = FakePlaybackController().apply { play(listOf(track(1), track(2)), 0) }
        val viewModel = subscribed(MiniPlayerViewModel(controller))
        val first = viewModel.state.value.trackKey

        controller.state.value = active(controller).copy(currentIndex = 1)

        assertEquals("Song 2", viewModel.state.value.title)
        assertNotEquals(first, viewModel.state.value.trackKey)
    }

    @Test
    fun `play-pause уходит в контроллер`() = runTest {
        val controller = FakePlaybackController().apply { play(listOf(track(1)), 0) }
        val viewModel = subscribed(MiniPlayerViewModel(controller))

        viewModel.onAction(MiniPlayerAction.TogglePlayPause)

        assertEquals(Command.TogglePlayPause, controller.commands.last())
    }

    @Test
    fun `перемотка - позиция в миллисекундах от длительности`() = runTest {
        val controller = FakePlaybackController().apply { play(listOf(track(1, durationMs = 200_000L)), 0) }
        val viewModel = subscribed(MiniPlayerViewModel(controller))

        viewModel.onAction(MiniPlayerAction.SeekTo(percent = 50f))

        assertEquals(Command.SeekTo(100_000L), controller.commands.last())
    }

    @Test
    fun `перемотка при неизвестной длительности не отправляется`() = runTest {
        val controller = FakePlaybackController().apply { play(listOf(track(1, durationMs = 0L)), 0) }
        val viewModel = subscribed(MiniPlayerViewModel(controller))

        viewModel.onAction(MiniPlayerAction.SeekTo(percent = 50f))

        assertTrue(controller.commands.none { it is Command.SeekTo })
    }

    /** The state is collected only while subscribed, as by the composable on screen. */
    private fun TestScope.subscribed(viewModel: MiniPlayerViewModel): MiniPlayerViewModel {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        return viewModel
    }

    private fun active(controller: FakePlaybackController) =
        controller.state.value as PlaybackState.Active

    private fun track(n: Int, durationMs: Long = 180_000L) =
        QueueItem("$n", TrackSource.DEEZER, "Song $n", "Artist $n", null, durationMs)
}
