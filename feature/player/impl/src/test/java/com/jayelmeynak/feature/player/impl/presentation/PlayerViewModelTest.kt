package com.jayelmeynak.feature.player.impl.presentation

import app.cash.turbine.test
import com.jayelmeynak.feature.player.api.PlaybackError
import com.jayelmeynak.feature.player.api.PlaybackState
import com.jayelmeynak.feature.player.api.QueueItem
import com.jayelmeynak.feature.player.api.TrackSource
import com.jayelmeynak.feature.player.api.testing.FakePlaybackController
import com.jayelmeynak.feature.player.api.testing.FakePlaybackController.Command
import com.jayelmeynak.feature.player.impl.R
import com.jayelmeynak.feature.player.impl.artwork.FakeLocalArtworkSource
import com.jayelmeynak.lib.designsystem.UiText
import com.jayelmeynak.util.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PlayerViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val artworks = FakeLocalArtworkSource()

    @Test
    fun `контроллер не подключён - Connecting, экран не закрывается`() = runTest {
        val controller = FakePlaybackController(connected = false)
        val viewModel = PlayerViewModel(controller, artworks)

        assertEquals(PlayerUiState.Connecting, viewModel.state.value)
        viewModel.effects.test { expectNoEvents() }
    }

    @Test
    fun `подключён с пустой очередью - Close ровно один раз`() = runTest {
        val controller = FakePlaybackController(connected = false)
        val viewModel = PlayerViewModel(controller, artworks)

        viewModel.effects.test {
            controller.connect()
            assertEquals(PlayerEffect.Close, awaitItem())
            controller.state.value = emptyActive().copy(isPlaying = true)
            controller.state.value = emptyActive()
            expectNoEvents()
        }
    }

    @Test
    fun `отложенный play до подключения - экран показывает трек, не закрывается`() = runTest {
        val controller = FakePlaybackController(connected = false)
        val viewModel = PlayerViewModel(controller, artworks)
        controller.play(listOf(deezer(1)), 0)

        viewModel.effects.test {
            controller.connect()
            expectNoEvents()
        }
        assertEquals("Deezer 1", ready(viewModel).title)
    }

    @Test
    fun `трек Deezer - подпись превью с длиной полного трека`() = runTest {
        val controller = FakePlaybackController()
        controller.play(listOf(deezer(1, durationMs = 224_000L)), 0)

        val state = ready(PlayerViewModel(controller, artworks))

        assertTrue(state.isPreview)
        assertEquals(224_000L, state.trackLengthMs)
        assertEquals("https://example.com/1.jpg", state.artworkUri)
    }

    @Test
    fun `локальный трек - без подписи превью, обложка из кэша обложек`() = runTest {
        artworks.artworks = mapOf(LOCAL_ID to byteArrayOf(7))
        val controller = FakePlaybackController()
        controller.play(listOf(local()), 0)

        val state = ready(PlayerViewModel(controller, artworks))

        assertFalse(state.isPreview)
        assertNull(state.artworkUri)
        assertArrayEquals(byteArrayOf(7), state.artworkData)
    }

    @Test
    fun `смена трека меняет заголовок и сбрасывает обложку прошлого трека`() = runTest {
        val pending = CompletableDeferred<ByteArray?>()
        artworks.artworks = mapOf(LOCAL_ID to byteArrayOf(7))
        val controller = FakePlaybackController()
        val queue = listOf(local(), local(id = OTHER_LOCAL_ID, title = "Other"))
        controller.play(queue, 0)
        val viewModel = PlayerViewModel(controller, artworks)
        assertArrayEquals(byteArrayOf(7), ready(viewModel).artworkData)
        artworks.pending[OTHER_LOCAL_ID] = pending

        controller.state.value = (controller.state.value as PlaybackState.Active).copy(currentIndex = 1)

        assertEquals("Other", ready(viewModel).title)
        assertNull(ready(viewModel).artworkData)
        pending.complete(byteArrayOf(9))
        assertArrayEquals(byteArrayOf(9), ready(viewModel).artworkData)
    }

    @Test
    fun `обложка прошлого трека, пришедшая после смены, не показывается`() = runTest {
        val late = CompletableDeferred<ByteArray?>()
        artworks.pending[LOCAL_ID] = late
        val controller = FakePlaybackController()
        controller.play(listOf(local(), deezer(2)), 0)
        val viewModel = PlayerViewModel(controller, artworks)

        controller.state.value = (controller.state.value as PlaybackState.Active).copy(currentIndex = 1)
        late.complete(byteArrayOf(7))

        assertNull(ready(viewModel).artworkData)
        assertEquals("https://example.com/2.jpg", ready(viewModel).artworkUri)
    }

    @Test
    fun `прогресс - доля позиции от длительности плеера`() = runTest {
        val controller = FakePlaybackController()
        controller.play(listOf(deezer(1)), 0)
        controller.state.value = (controller.state.value as PlaybackState.Active).copy(durationMs = 30_000L)
        val viewModel = PlayerViewModel(controller, artworks)

        controller.positionMs.value = 15_000L

        assertEquals(50f, ready(viewModel).progress)
        assertEquals(15_000L, ready(viewModel).positionMs)
        assertEquals(30_000L, ready(viewModel).durationMs)
    }

    @Test
    fun `неизвестная длительность - прогресс 0, перемотка по доле не отправляется`() = runTest {
        val controller = FakePlaybackController()
        controller.play(listOf(deezer(1)), 0)
        controller.state.value = (controller.state.value as PlaybackState.Active).copy(durationMs = 0L)
        val viewModel = PlayerViewModel(controller, artworks)
        controller.positionMs.value = 5_000L

        viewModel.onAction(PlayerAction.SeekTo(50f))

        assertEquals(0f, ready(viewModel).progress)
        assertTrue(controller.commands.none { it is Command.SeekTo })
    }

    @Test
    fun `перемотка после жеста - одна команда с позицией по доле`() = runTest {
        val controller = FakePlaybackController()
        controller.play(listOf(deezer(1)), 0)
        controller.state.value = (controller.state.value as PlaybackState.Active).copy(durationMs = 30_000L)
        val viewModel = PlayerViewModel(controller, artworks)

        viewModel.onAction(PlayerAction.SeekTo(25f))

        assertEquals(listOf(Command.SeekTo(7_500L)), controller.commands.filterIsInstance<Command.SeekTo>())
    }

    @Test
    fun `кнопки транспорта уходят в контроллер`() = runTest {
        val controller = FakePlaybackController()
        controller.play(listOf(deezer(1)), 0)
        val viewModel = PlayerViewModel(controller, artworks)

        viewModel.onAction(PlayerAction.TogglePlayPause)
        viewModel.onAction(PlayerAction.Next)
        viewModel.onAction(PlayerAction.Previous)
        viewModel.onAction(PlayerAction.SeekBack)
        viewModel.onAction(PlayerAction.SeekForward)

        assertEquals(
            listOf(Command.TogglePlayPause, Command.Next, Command.Previous, Command.SeekBack, Command.SeekForward),
            controller.commands.drop(1),
        )
    }

    @Test
    fun `ошибка трека - сообщение на экране, экран не закрывается`() = runTest {
        val controller = FakePlaybackController()
        controller.play(listOf(deezer(1)), 0)
        val viewModel = PlayerViewModel(controller, artworks)

        viewModel.effects.test {
            controller.state.value = (controller.state.value as PlaybackState.Active)
                .copy(error = PlaybackError.SOURCE_UNAVAILABLE, isPlaying = false)
            expectNoEvents()
        }
        assertEquals(
            R.string.player_error_source_unavailable,
            (ready(viewModel).errorMessage as UiText.StringResourceId).id,
        )
    }

    @Test
    fun `потеря подключения после показа трека - Connecting, без закрытия`() = runTest {
        val controller = FakePlaybackController()
        controller.play(listOf(deezer(1)), 0)
        val viewModel = PlayerViewModel(controller, artworks)

        viewModel.effects.test {
            controller.state.value = PlaybackState.Idle
            expectNoEvents()
        }
        assertEquals(PlayerUiState.Connecting, viewModel.state.value)
    }

    @Test
    fun `текущий элемент чужой - экран с управлением без метаданных, без закрытия`() = runTest {
        val controller = FakePlaybackController()
        controller.play(listOf(deezer(1)), 0)
        val viewModel = PlayerViewModel(controller, artworks)

        viewModel.effects.test {
            controller.state.value = (controller.state.value as PlaybackState.Active).copy(currentIndex = -1)
            expectNoEvents()
        }
        val state = ready(viewModel)
        assertEquals("", state.title)
        assertFalse(state.isPreview)
        assertFalse(state.showPlayButton)
    }

    @Test
    fun `буферизация при запрошенном воспроизведении - кнопка паузы и индикатор, тап уходит в контроллер`() = runTest {
        val controller = FakePlaybackController()
        controller.play(listOf(deezer(1)), 0)
        val viewModel = PlayerViewModel(controller, artworks)

        controller.state.value = (controller.state.value as PlaybackState.Active)
            .copy(isPlaying = false, playWhenReady = true, isBuffering = true, showPlayButton = false)

        val state = ready(viewModel)
        assertFalse(state.showPlayButton)
        assertTrue(state.isBuffering)
        viewModel.onAction(PlayerAction.TogglePlayPause)
        assertEquals(Command.TogglePlayPause, controller.commands.last())
    }

    @Test
    fun `пауза и ошибка - кнопка воспроизведения`() = runTest {
        val controller = FakePlaybackController()
        controller.play(listOf(deezer(1)), 0)
        val viewModel = PlayerViewModel(controller, artworks)
        val playing = controller.state.value as PlaybackState.Active

        controller.state.value = playing.copy(isPlaying = false, playWhenReady = false, showPlayButton = true)
        assertTrue(ready(viewModel).showPlayButton)

        controller.state.value = playing.copy(
            isPlaying = false,
            playWhenReady = true,
            showPlayButton = true,
            error = PlaybackError.SOURCE_UNAVAILABLE,
        )
        assertTrue(ready(viewModel).showPlayButton)
    }

    private fun ready(viewModel: PlayerViewModel): PlayerUiState.Ready =
        viewModel.state.value as PlayerUiState.Ready

    private fun emptyActive() = PlaybackState.Active(
        queue = emptyList(),
        currentIndex = 0,
        isPlaying = false,
        playWhenReady = false,
        isBuffering = false,
        showPlayButton = true,
        durationMs = 0L,
        error = null,
    )

    private fun deezer(id: Int, durationMs: Long = 180_000L) = QueueItem(
        id = id.toString(),
        source = TrackSource.DEEZER,
        title = "Deezer $id",
        artist = "Artist $id",
        artworkUri = "https://example.com/$id.jpg",
        durationMs = durationMs,
    )

    private fun local(id: String = LOCAL_ID, title: String = "Local") = QueueItem(
        id = id,
        source = TrackSource.LOCAL,
        title = title,
        artist = "Someone",
        artworkUri = id,
        durationMs = 100_000L,
    )

    private companion object {
        const val LOCAL_ID = "content://media/external/audio/media/7"
        const val OTHER_LOCAL_ID = "content://media/external/audio/media/8"
    }
}
