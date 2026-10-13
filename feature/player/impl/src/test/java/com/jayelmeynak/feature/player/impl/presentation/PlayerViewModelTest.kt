package com.jayelmeynak.feature.player.impl.presentation

import app.cash.turbine.test
import com.jayelmeynak.feature.player.api.PlaybackError
import com.jayelmeynak.feature.player.api.PlaybackState
import com.jayelmeynak.feature.player.api.QueueItem
import com.jayelmeynak.feature.player.api.TrackSource
import com.jayelmeynak.feature.player.api.testing.FakePlaybackController
import com.jayelmeynak.feature.player.api.testing.FakePlaybackController.Command
import com.jayelmeynak.feature.player.impl.R
import com.jayelmeynak.lib.designsystem.UiText
import com.jayelmeynak.util.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PlayerViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val promptStore = FakeNotificationPromptStore()

    @Test
    fun `контроллер не подключён - Connecting, экран не закрывается`() = runTest {
        val controller = FakePlaybackController(connected = false)
        val viewModel = PlayerViewModel(controller, promptStore)

        assertEquals(PlayerUiState.Connecting, viewModel.state.value)
        viewModel.effects.test { expectNoEvents() }
    }

    @Test
    fun `подключён с пустой очередью - Close ровно один раз`() = runTest {
        val controller = FakePlaybackController(connected = false)
        val viewModel = PlayerViewModel(controller, promptStore)

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
        val viewModel = PlayerViewModel(controller, promptStore)
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

        val state = ready(PlayerViewModel(controller, promptStore))

        assertTrue(state.isPreview)
        assertEquals(224_000L, state.trackLengthMs)
        assertEquals("https://example.com/1.jpg", state.artworkUri)
    }

    @Test
    fun `локальный трек - без подписи превью, обложка - его content URI`() = runTest {
        val controller = FakePlaybackController()
        controller.play(listOf(local()), 0)

        val state = ready(PlayerViewModel(controller, promptStore))

        assertFalse(state.isPreview)
        assertEquals(LOCAL_ID, state.artworkUri)
    }

    @Test
    fun `смена трека меняет заголовок и обложку`() = runTest {
        val controller = FakePlaybackController()
        controller.play(listOf(local(), local(id = OTHER_LOCAL_ID, title = "Other"), deezer(2)), 0)
        val viewModel = PlayerViewModel(controller, promptStore)
        assertEquals(LOCAL_ID, ready(viewModel).artworkUri)

        controller.state.value = (controller.state.value as PlaybackState.Active).copy(currentIndex = 1)
        assertEquals("Other", ready(viewModel).title)
        assertEquals(OTHER_LOCAL_ID, ready(viewModel).artworkUri)

        controller.state.value = (controller.state.value as PlaybackState.Active).copy(currentIndex = 2)
        assertEquals("https://example.com/2.jpg", ready(viewModel).artworkUri)
    }

    @Test
    fun `прогресс - доля позиции от длительности плеера`() = runTest {
        val controller = FakePlaybackController()
        controller.play(listOf(deezer(1)), 0)
        controller.state.value = (controller.state.value as PlaybackState.Active).copy(durationMs = 30_000L)
        val viewModel = PlayerViewModel(controller, promptStore)

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
        val viewModel = PlayerViewModel(controller, promptStore)
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
        val viewModel = PlayerViewModel(controller, promptStore)

        viewModel.onAction(PlayerAction.SeekTo(25f))

        assertEquals(listOf(Command.SeekTo(7_500L)), controller.commands.filterIsInstance<Command.SeekTo>())
    }

    @Test
    fun `кнопки транспорта уходят в контроллер`() = runTest {
        val controller = FakePlaybackController()
        controller.play(listOf(deezer(1)), 0)
        val viewModel = PlayerViewModel(controller, promptStore)

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
    fun `ошибка сети - своё сообщение на экране`() = runTest {
        val controller = FakePlaybackController()
        controller.play(listOf(deezer(1)), 0)
        val viewModel = PlayerViewModel(controller, promptStore)

        controller.state.value = (controller.state.value as PlaybackState.Active)
            .copy(error = PlaybackError.NETWORK, isPlaying = false)

        assertEquals(UiText.StringResourceId(R.string.player_error_network), ready(viewModel).errorMessage)
    }

    @Test
    fun `ошибка трека - сообщение на экране, экран не закрывается`() = runTest {
        val controller = FakePlaybackController()
        controller.play(listOf(deezer(1)), 0)
        val viewModel = PlayerViewModel(controller, promptStore)

        viewModel.effects.test {
            controller.state.value = (controller.state.value as PlaybackState.Active)
                .copy(error = PlaybackError.SOURCE_UNAVAILABLE, isPlaying = false)
            expectNoEvents()
        }
        assertEquals(
            UiText.StringResourceId(R.string.player_error_source_unavailable),
            ready(viewModel).errorMessage,
        )
    }

    @Test
    fun `потеря подключения после показа трека - Connecting, без закрытия`() = runTest {
        val controller = FakePlaybackController()
        controller.play(listOf(deezer(1)), 0)
        val viewModel = PlayerViewModel(controller, promptStore)

        viewModel.effects.test {
            controller.state.value = PlaybackState.Idle
            expectNoEvents()
        }
        assertEquals(PlayerUiState.Connecting, viewModel.state.value)
    }

    @Test
    fun `уход в фон - трек остаётся на экране, без закрытия`() = runTest {
        val controller = FakePlaybackController()
        controller.play(listOf(deezer(1)), 0)
        val viewModel = PlayerViewModel(controller, promptStore)

        viewModel.effects.test {
            controller.disconnect()
            expectNoEvents()
        }
        assertEquals("Deezer 1", ready(viewModel).title)
    }

    @Test
    fun `не подключённый снапшот с пустой очередью - экран ждёт, не закрывается`() = runTest {
        val controller = FakePlaybackController()
        val viewModel = PlayerViewModel(controller, promptStore)
        viewModel.effects.test {
            // Подключённая пустая сессия закрывает экран; здесь снапшот уже не подключён.
            controller.state.value = emptyActive().copy(isConnected = false)
            cancelAndIgnoreRemainingEvents()
        }
        val fresh = PlayerViewModel(controller, promptStore)

        fresh.effects.test { expectNoEvents() }
        assertEquals(PlayerUiState.Connecting, fresh.state.value)
    }

    @Test
    fun `подключиться не удалось - экран показывает ошибку подключения, без закрытия`() = runTest {
        val controller = FakePlaybackController(connected = false)
        val viewModel = PlayerViewModel(controller, promptStore)

        viewModel.effects.test {
            controller.failConnection()
            expectNoEvents()
        }
        assertEquals(PlayerUiState.ConnectionFailed, viewModel.state.value)
    }

    @Test
    fun `текущий элемент чужой - экран с управлением без метаданных, без закрытия`() = runTest {
        val controller = FakePlaybackController()
        controller.play(listOf(deezer(1)), 0)
        val viewModel = PlayerViewModel(controller, promptStore)

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
        val viewModel = PlayerViewModel(controller, promptStore)

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
        val viewModel = PlayerViewModel(controller, promptStore)
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

    @Test
    fun `нет разрешения уведомлений и ещё не спрашивали - один системный запрос`() = runTest {
        val controller = FakePlaybackController().apply { play(listOf(deezer(1)), 0) }
        val viewModel = PlayerViewModel(controller, promptStore)

        viewModel.effects.test {
            viewModel.onAction(PlayerAction.OnNotificationPermissionChecked(granted = false))
            assertEquals(PlayerEffect.RequestNotificationPermission, awaitItem())
            assertTrue(promptStore.prompted)

            // Повторный показ экрана (новая проверка) — запрос уже был.
            viewModel.onAction(PlayerAction.OnNotificationPermissionChecked(granted = false))
            expectNoEvents()
        }
    }

    @Test
    fun `две проверки подряд до ответа хранилища - один запрос`() = runTest {
        val store = FakeNotificationPromptStore(gate = CompletableDeferred())
        val controller = FakePlaybackController().apply { play(listOf(deezer(1)), 0) }
        val viewModel = PlayerViewModel(controller, store)

        viewModel.effects.test {
            // Поворот сразу после открытия: вторая проверка приходит, пока первая ещё ждёт хранилище.
            viewModel.onAction(PlayerAction.OnNotificationPermissionChecked(granted = false))
            viewModel.onAction(PlayerAction.OnNotificationPermissionChecked(granted = false))
            store.gate?.complete(Unit)

            assertEquals(PlayerEffect.RequestNotificationPermission, awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `уже спрашивали при прошлом запуске - запроса нет`() = runTest {
        promptStore.prompted = true
        val controller = FakePlaybackController().apply { play(listOf(deezer(1)), 0) }
        val viewModel = PlayerViewModel(controller, promptStore)

        viewModel.effects.test {
            viewModel.onAction(PlayerAction.OnNotificationPermissionChecked(granted = false))
            expectNoEvents()
        }
    }

    @Test
    fun `разрешение уведомлений есть - без запроса и без отметки`() = runTest {
        val controller = FakePlaybackController().apply { play(listOf(deezer(1)), 0) }
        val viewModel = PlayerViewModel(controller, promptStore)

        viewModel.effects.test {
            viewModel.onAction(PlayerAction.OnNotificationPermissionChecked(granted = true))
            expectNoEvents()
        }
        assertFalse(promptStore.prompted)
    }

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
