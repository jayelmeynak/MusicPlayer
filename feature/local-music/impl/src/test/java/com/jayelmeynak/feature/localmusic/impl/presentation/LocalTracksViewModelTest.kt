package com.jayelmeynak.feature.localmusic.impl.presentation

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.jayelmeynak.feature.localmusic.impl.R
import com.jayelmeynak.feature.localmusic.impl.domain.model.LocalTrack
import com.jayelmeynak.feature.localmusic.impl.domain.usecase.GetLocalTracksUseCase
import com.jayelmeynak.feature.localmusic.impl.domain.usecase.LIBRARY_CHANGES_DEBOUNCE_MS
import com.jayelmeynak.feature.localmusic.impl.domain.usecase.ObserveLibraryChangesUseCase
import com.jayelmeynak.feature.localmusic.impl.domain.usecase.SearchLocalTracksUseCase
import com.jayelmeynak.feature.localmusic.impl.testing.FakeLocalTracksRepository
import com.jayelmeynak.feature.player.api.testing.FakePlaybackController
import com.jayelmeynak.feature.player.api.testing.FakePlaybackController.Command
import com.jayelmeynak.lib.designsystem.UiText
import com.jayelmeynak.lib.designsystem.search.SEARCH_DEBOUNCE_MS
import com.jayelmeynak.util.result.DataError
import com.jayelmeynak.util.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LocalTracksViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val yesterday = localTrack(1, title = "Yesterday", artist = "The Beatles")
    private val numb = localTrack(2, title = "Numb", artist = "Linkin Park")
    private val help = localTrack(3, title = "Help!", artist = "The Beatles")

    private val repository = FakeLocalTracksRepository(tracks = listOf(yesterday, numb, help))
    private val playbackController = FakePlaybackController()

    /** VM вкладки; [granted] — экран уже проверил доступ и он есть (первая загрузка — по нему). */
    private fun viewModel(granted: Boolean = true) = LocalTracksViewModel(
        GetLocalTracksUseCase(repository),
        SearchLocalTracksUseCase(),
        ObserveLibraryChangesUseCase(repository),
        playbackController,
    ).apply {
        if (granted) onAction(LocalTracksAction.OnAudioPermissionChecked(granted = true))
    }

    @Test
    fun `загрузка - треки медиатеки`() = runTest {
        viewModel().state.test {
            val state = awaitLoaded()
            assertEquals(listOf(yesterday, numb, help), state.tracks)
            assertEquals(LocalTracksContent.Tracks(listOf(yesterday, numb, help)), state.content)
        }
    }

    @Test
    fun `нет треков - пустое состояние`() = runTest {
        repository.tracks = emptyList()

        viewModel().state.test {
            val state = awaitLoaded()
            assertTrue(state.tracks.isEmpty())
            assertEquals(LocalTracksContent.Empty, state.content)
        }
    }

    @Test
    fun `поиск по исполнителю без учёта регистра после паузы ввода`() = runTest {
        val viewModel = viewModel()

        viewModel.state.test {
            awaitLoaded()
            viewModel.onAction(LocalTracksAction.OnSearchQueryChange("beatles"))
            assertEquals("beatles", expectMostRecentItem().query)

            advanceTimeBy(SEARCH_DEBOUNCE_MS - 1)
            assertEquals(LocalSearchState.Inactive, viewModel.state.value.search)

            advanceTimeBy(2)
            assertEquals(LocalSearchState.Results(listOf(yesterday, help)), expectMostRecentItem().search)
        }
    }

    @Test
    fun `поиск по подстроке названия без учёта регистра`() = runTest {
        val viewModel = viewModel()

        viewModel.state.test {
            awaitLoaded()
            viewModel.onAction(LocalTracksAction.OnSearchQueryChange("NUM"))
            advanceTimeBy(SEARCH_DEBOUNCE_MS + 1)

            assertEquals(LocalSearchState.Results(listOf(numb)), expectMostRecentItem().search)
        }
    }

    @Test
    fun `поиск без совпадений - пустой результат, а не отсутствие поиска`() = runTest {
        val viewModel = viewModel()

        viewModel.state.test {
            awaitLoaded()
            viewModel.onAction(LocalTracksAction.OnSearchQueryChange("zzzzzz"))
            advanceTimeBy(SEARCH_DEBOUNCE_MS + 1)

            val state = expectMostRecentItem()
            assertEquals(LocalSearchState.Results(emptyList()), state.search)
            assertEquals(LocalTracksContent.NothingFound, state.content)
        }
    }

    @Test
    fun `очистка строки сразу убирает результат поиска`() = runTest {
        val viewModel = viewModel()

        viewModel.state.test {
            awaitLoaded()
            viewModel.onAction(LocalTracksAction.OnSearchQueryChange("help"))
            advanceTimeBy(SEARCH_DEBOUNCE_MS + 1)
            viewModel.onAction(LocalTracksAction.OnSearchQueryChange(""))

            assertEquals(LocalSearchState.Inactive, expectMostRecentItem().search)
        }
    }

    @Test
    fun `ошибка загрузки - прежний текст «Не удалось загрузить треки», список пуст`() = runTest {
        repository.tracksError = DataError.Local.UNKNOWN

        viewModel().state.test {
            val state = awaitLoaded()
            assertEquals(
                LocalTracksContent.Error(UiText.StringResourceId(R.string.local_tracks_load_error)),
                state.content,
            )
            assertTrue(state.tracks.isEmpty())
        }
    }

    @Test
    fun `нет разрешения на аудио - состояние без доступа`() = runTest {
        val viewModel = viewModel()

        viewModel.state.test {
            awaitLoaded()
            viewModel.onAction(LocalTracksAction.OnAudioPermissionChecked(granted = false))

            val state = expectMostRecentItem()
            assertEquals(AudioAccess.Denied(permanently = false), state.access)
            assertEquals(LocalTracksContent.NoAccess(permanently = false), state.content)
        }
    }

    @Test
    fun `разрешение выдано после отказа - список загружается заново`() = runTest {
        val viewModel = viewModel()

        viewModel.state.test {
            awaitLoaded()
            viewModel.onAction(LocalTracksAction.OnAudioPermissionChecked(granted = false))
            viewModel.onAction(LocalTracksAction.OnAudioPermissionChecked(granted = true))

            val state = awaitState { it.isLoadFinished && it.access == AudioAccess.Granted }
            assertEquals(2, repository.tracksRequests)
            assertEquals(3, state.tracks.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `ошибка загрузки и разрешение есть - список загружается заново`() = runTest {
        repository.tracksError = DataError.Local.UNKNOWN
        val viewModel = viewModel()

        viewModel.state.test {
            awaitLoaded()
            repository.tracksError = null
            viewModel.onAction(LocalTracksAction.OnAudioPermissionChecked(granted = true))

            val state = awaitState { it.isLoadFinished && it.tracks.size == 3 }
            assertEquals(AudioAccess.Granted, state.access)
            assertEquals(LocalTracksContent.Tracks(listOf(yesterday, numb, help)), state.content)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `разрешение есть и список загружен - повторной загрузки нет`() = runTest {
        val viewModel = viewModel()

        viewModel.state.test {
            awaitLoaded()
            viewModel.onAction(LocalTracksAction.OnAudioPermissionChecked(granted = true))

            assertEquals(1, repository.tracksRequests)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `поиск, набранный до перезагрузки, применяется к новым трекам`() = runTest {
        repository.tracksError = DataError.Local.UNKNOWN
        val viewModel = viewModel()

        viewModel.state.test {
            awaitLoaded()
            viewModel.onAction(LocalTracksAction.OnSearchQueryChange("help"))
            advanceTimeBy(SEARCH_DEBOUNCE_MS + 1)
            repository.tracksError = null
            viewModel.onAction(LocalTracksAction.OnAudioPermissionChecked(granted = true))

            val state = awaitState { it.isLoadFinished && it.tracks.isNotEmpty() }
            assertEquals(LocalSearchState.Results(listOf(help)), state.search)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `очистка и повтор того же запроса внутри паузы - результаты видны`() = runTest {
        val viewModel = viewModel()

        viewModel.state.test {
            awaitLoaded()
            viewModel.onAction(LocalTracksAction.OnSearchQueryChange("help"))
            advanceTimeBy(SEARCH_DEBOUNCE_MS + 1)
            viewModel.onAction(LocalTracksAction.OnSearchQueryChange(""))
            advanceTimeBy(100)
            viewModel.onAction(LocalTracksAction.OnSearchQueryChange("help"))
            advanceTimeBy(SEARCH_DEBOUNCE_MS + 1)

            assertEquals(LocalSearchState.Results(listOf(help)), expectMostRecentItem().search)
        }
    }

    @Test
    fun `пустой запрос делает поиск неактивным`() = runTest {
        val viewModel = viewModel()

        viewModel.state.test {
            awaitLoaded()
            viewModel.onAction(LocalTracksAction.OnSearchQueryChange("help"))
            advanceTimeBy(SEARCH_DEBOUNCE_MS + 1)
            viewModel.onAction(LocalTracksAction.OnSearchQueryChange(""))
            advanceTimeBy(SEARCH_DEBOUNCE_MS + 1)

            val state = expectMostRecentItem()
            assertEquals(LocalSearchState.Inactive, state.search)
            assertEquals("", state.query)
        }
    }

    @Test
    fun `треки есть, ошибка или нет доступа - не пустое состояние`() = runTest {
        val viewModel = viewModel()

        viewModel.state.test {
            assertNotEquals(LocalTracksContent.Empty, awaitLoaded().content)
            viewModel.onAction(LocalTracksAction.OnAudioPermissionChecked(granted = false))
            assertNotEquals(LocalTracksContent.Empty, expectMostRecentItem().content)
        }
        repository.tracksError = DataError.Local.UNKNOWN
        viewModel().state.test {
            assertNotEquals(LocalTracksContent.Empty, awaitLoaded().content)
        }
    }

    @Test
    fun `тап по треку списка запускает нарисованный список с индексом трека`() = runTest {
        val viewModel = viewModel()

        viewModel.state.test {
            val tracks = awaitLoaded().tracks
            viewModel.onAction(LocalTracksAction.OnTrackClick(tracks, 1))

            assertEquals(
                listOf(Command.Play(tracks.map { it.toQueueItem() }, 1)),
                playbackController.commands,
            )
        }
    }

    @Test
    fun `тап по треку выдачи поиска запускает выдачу`() = runTest {
        val viewModel = viewModel()

        viewModel.state.test {
            awaitLoaded()
            viewModel.onAction(LocalTracksAction.OnSearchQueryChange("beatles"))
            advanceTimeBy(SEARCH_DEBOUNCE_MS + 1)
            val found = (expectMostRecentItem().search as LocalSearchState.Results).tracks
            viewModel.onAction(LocalTracksAction.OnTrackClick(found, 1))

            assertEquals(
                listOf(Command.Play(listOf(yesterday, help).map { it.toQueueItem() }, 1)),
                playbackController.commands,
            )
        }
    }

    @Test
    fun `отказ в доступе во время загрузки отменяет её`() = runTest {
        val pending = CompletableDeferred<Unit>()
        repository.pendingTracks = pending
        val viewModel = viewModel()
        assertEquals(LibraryState.Loading, viewModel.state.value.library)

        viewModel.onAction(LocalTracksAction.OnAudioPermissionChecked(granted = false))
        pending.complete(Unit)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(LibraryState.Loading, state.library)
        assertEquals(LocalTracksContent.NoAccess(permanently = false), state.content)
        assertEquals(1, repository.tracksRequests)
    }

    @Test
    fun `повторная проверка доступа во время загрузки не перезапускает её`() = runTest {
        val pending = CompletableDeferred<Unit>()
        repository.pendingTracks = pending
        val viewModel = viewModel()

        viewModel.onAction(LocalTracksAction.OnAudioPermissionChecked(granted = true))
        assertEquals(1, repository.tracksRequests)
        pending.complete(Unit)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(1, repository.tracksRequests)
        assertEquals(AudioAccess.Granted, state.access)
        assertEquals(LibraryState.Loaded(listOf(yesterday, numb, help)), state.library)
    }

    @Test
    fun `поиск, сработавший во время загрузки, после загрузки даёт выдачу по трекам`() = runTest {
        val pending = CompletableDeferred<Unit>()
        repository.pendingTracks = pending
        val viewModel = viewModel()

        viewModel.onAction(LocalTracksAction.OnSearchQueryChange("beatles"))
        advanceTimeBy(SEARCH_DEBOUNCE_MS + 1)
        assertEquals(LocalTracksContent.Loading, viewModel.state.value.content)

        pending.complete(Unit)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(LocalSearchState.Results(listOf(yesterday, help)), state.search)
        assertEquals(LocalTracksContent.Tracks(listOf(yesterday, help)), state.content)
    }

    @Test
    fun `тап по треку - сначала play, затем эффект открытия плеера`() = runTest {
        val viewModel = viewModel()
        val tracks = listOf(yesterday, numb, help)
        // Сколько команд play фейк получил к моменту эффекта: сборщик без диспетчеризации получает
        // эффект прямо в отправке, поэтому видит состояние фейка на тот момент.
        val playsAtEffect = mutableListOf<Int>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effect ->
                assertEquals(LocalTracksEffect.OpenPlayer, effect)
                playsAtEffect += playbackController.commands.count { it is Command.Play }
            }
        }

        viewModel.onAction(LocalTracksAction.OnTrackClick(tracks, 2))

        assertEquals(listOf(1), playsAtEffect)
    }

    @Test
    fun `без тапа эффектов нет`() = runTest {
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.onAction(LocalTracksAction.OnSearchQueryChange("help"))
            advanceTimeBy(SEARCH_DEBOUNCE_MS + 1)
            viewModel.onAction(LocalTracksAction.OnAudioPermissionChecked(granted = false))
            expectNoEvents()
        }
    }

    @Test
    fun `до проверки доступа медиатека не читается`() = runTest {
        val viewModel = viewModel(granted = false)
        advanceUntilIdle()

        assertEquals(0, repository.tracksRequests)
        assertEquals(AudioAccess.Unknown, viewModel.state.value.access)
        assertEquals(LocalTracksContent.Loading, viewModel.state.value.content)
    }

    @Test
    fun `первый доступ - первая загрузка`() = runTest {
        val viewModel = viewModel(granted = false)

        viewModel.onAction(LocalTracksAction.OnAudioPermissionChecked(granted = true))
        advanceUntilIdle()

        assertEquals(1, repository.tracksRequests)
        assertEquals(LibraryState.Loaded(listOf(yesterday, numb, help)), viewModel.state.value.library)
    }

    @Test
    fun `первая проверка без доступа - системный запрос один раз`() = runTest {
        val viewModel = viewModel(granted = false)

        viewModel.effects.test {
            viewModel.onAction(LocalTracksAction.OnAudioPermissionChecked(granted = false))
            assertEquals(LocalTracksEffect.RequestAudioPermission, awaitItem())
            assertEquals(AudioAccess.Denied(permanently = false), viewModel.state.value.access)

            // Следующий resume (после диалога) не спрашивает сам.
            viewModel.onAction(LocalTracksAction.OnAudioPermissionChecked(granted = false))
            expectNoEvents()
        }
        assertEquals(0, repository.tracksRequests)
    }

    @Test
    fun `отказ в запросе без возможности спросить снова - окончательный отказ`() = runTest {
        val viewModel = viewModel(granted = false)
        viewModel.onAction(LocalTracksAction.OnAudioPermissionChecked(granted = false))

        viewModel.onAction(LocalTracksAction.OnAudioPermissionResult(granted = false, rationaleBeforeRequest = true, rationaleAfterRequest = false))
        assertEquals(AudioAccess.Denied(permanently = true), viewModel.state.value.access)
        assertEquals(LocalTracksContent.NoAccess(permanently = true), viewModel.state.value.content)

        // Проверка на resume признак не сбрасывает.
        viewModel.onAction(LocalTracksAction.OnAudioPermissionChecked(granted = false))
        assertEquals(AudioAccess.Denied(permanently = true), viewModel.state.value.access)
    }

    @Test
    fun `отказ в запросе, который можно повторить, - обычный отказ`() = runTest {
        val viewModel = viewModel(granted = false)
        viewModel.onAction(LocalTracksAction.OnAudioPermissionChecked(granted = false))

        viewModel.onAction(LocalTracksAction.OnAudioPermissionResult(granted = false, rationaleBeforeRequest = false, rationaleAfterRequest = true))

        assertEquals(AudioAccess.Denied(permanently = false), viewModel.state.value.access)
    }

    @Test
    fun `первый диалог закрыт без ответа - обычный отказ, кнопка снова спрашивает`() = runTest {
        val viewModel = viewModel(granted = false)
        viewModel.onAction(LocalTracksAction.OnAudioPermissionChecked(granted = false))

        // Back или тап мимо диалога: отказ без rationale ни до, ни после — это не «больше не спрашивать».
        viewModel.onAction(
            LocalTracksAction.OnAudioPermissionResult(granted = false, rationaleBeforeRequest = false, rationaleAfterRequest = false)
        )

        assertEquals(AudioAccess.Denied(permanently = false), viewModel.state.value.access)
    }

    @Test
    fun `«больше не спрашивать» после первого отказа - окончательный отказ`() = runTest {
        val viewModel = viewModel(granted = false)
        viewModel.onAction(LocalTracksAction.OnAudioPermissionChecked(granted = false))
        viewModel.onAction(
            LocalTracksAction.OnAudioPermissionResult(granted = false, rationaleBeforeRequest = false, rationaleAfterRequest = true)
        )
        viewModel.onAction(LocalTracksAction.OnGrantAccessClick)

        viewModel.onAction(
            LocalTracksAction.OnAudioPermissionResult(granted = false, rationaleBeforeRequest = true, rationaleAfterRequest = false)
        )

        assertEquals(AudioAccess.Denied(permanently = true), viewModel.state.value.access)
    }

    @Test
    fun `отказ без диалога после прошлого отказа в этой VM - окончательный отказ`() = runTest {
        // Окончательный отказ из прошлой версии: система отвечает отказом без диалога, rationale — false всегда.
        val viewModel = viewModel(granted = false)
        viewModel.onAction(LocalTracksAction.OnAudioPermissionChecked(granted = false))
        viewModel.onAction(
            LocalTracksAction.OnAudioPermissionResult(granted = false, rationaleBeforeRequest = false, rationaleAfterRequest = false)
        )
        assertEquals(AudioAccess.Denied(permanently = false), viewModel.state.value.access)

        viewModel.onAction(LocalTracksAction.OnGrantAccessClick)
        viewModel.onAction(
            LocalTracksAction.OnAudioPermissionResult(granted = false, rationaleBeforeRequest = false, rationaleAfterRequest = false)
        )

        assertEquals(AudioAccess.Denied(permanently = true), viewModel.state.value.access)
    }

    @Test
    fun `кнопка после обычного отказа - снова системный запрос`() = runTest {
        val viewModel = viewModel(granted = false)
        viewModel.onAction(LocalTracksAction.OnAudioPermissionChecked(granted = false))
        viewModel.onAction(LocalTracksAction.OnAudioPermissionResult(granted = false, rationaleBeforeRequest = false, rationaleAfterRequest = true))

        viewModel.effects.test {
            assertEquals(LocalTracksEffect.RequestAudioPermission, awaitItem())
            viewModel.onAction(LocalTracksAction.OnGrantAccessClick)
            assertEquals(LocalTracksEffect.RequestAudioPermission, awaitItem())
        }
    }

    @Test
    fun `кнопка после окончательного отказа - настройки приложения`() = runTest {
        val viewModel = viewModel(granted = false)
        viewModel.onAction(LocalTracksAction.OnAudioPermissionChecked(granted = false))
        viewModel.onAction(LocalTracksAction.OnAudioPermissionResult(granted = false, rationaleBeforeRequest = true, rationaleAfterRequest = false))

        viewModel.effects.test {
            assertEquals(LocalTracksEffect.RequestAudioPermission, awaitItem())
            viewModel.onAction(LocalTracksAction.OnGrantAccessClick)
            assertEquals(LocalTracksEffect.OpenAppSettings, awaitItem())
        }
    }

    @Test
    fun `доступ выдан в системном диалоге - список загружается`() = runTest {
        val viewModel = viewModel(granted = false)
        viewModel.onAction(LocalTracksAction.OnAudioPermissionChecked(granted = false))

        viewModel.onAction(LocalTracksAction.OnAudioPermissionResult(granted = true, rationaleBeforeRequest = false, rationaleAfterRequest = false))
        advanceUntilIdle()

        assertEquals(AudioAccess.Granted, viewModel.state.value.access)
        assertEquals(1, repository.tracksRequests)
        assertEquals(LibraryState.Loaded(listOf(yesterday, numb, help)), viewModel.state.value.library)
    }

    @Test
    fun `до доступа изменения медиатеки не наблюдаются, после - одна подписка`() = runTest {
        val viewModel = viewModel(granted = false)
        viewModel.onAction(LocalTracksAction.OnAudioPermissionChecked(granted = false))
        advanceUntilIdle()
        assertEquals(0, repository.changes.subscriptionCount.value)

        viewModel.onAction(LocalTracksAction.OnAudioPermissionChecked(granted = true))
        viewModel.onAction(LocalTracksAction.OnAudioPermissionChecked(granted = false))
        viewModel.onAction(LocalTracksAction.OnAudioPermissionChecked(granted = true))
        advanceUntilIdle()

        assertEquals(1, repository.changes.subscriptionCount.value)
    }

    @Test
    fun `изменение медиатеки - список перезагружается без индикатора загрузки`() = runTest {
        val viewModel = viewModel()

        viewModel.state.test {
            awaitLoaded()
            repository.tracks = listOf(numb)
            repository.changes.emit(Unit)
            repository.changes.emit(Unit)
            advanceTimeBy(LIBRARY_CHANGES_DEBOUNCE_MS + 1)

            // Следующее состояние — сразу новый список: прежний виден, пока идёт перезагрузка.
            assertEquals(LibraryState.Loaded(listOf(numb)), awaitItem().library)
            assertEquals(2, repository.tracksRequests)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `изменение медиатеки без доступа - перезагрузки нет`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onAction(LocalTracksAction.OnAudioPermissionChecked(granted = false))

        repository.changes.emit(Unit)
        advanceTimeBy(LIBRARY_CHANGES_DEBOUNCE_MS + 1)

        assertEquals(1, repository.tracksRequests)
    }

    @Test
    fun `изменение медиатеки и ошибка чтения - ошибка загрузки`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        repository.tracksError = DataError.Local.UNKNOWN

        repository.changes.emit(Unit)
        advanceTimeBy(LIBRARY_CHANGES_DEBOUNCE_MS + 1)

        assertEquals(
            LibraryState.Failed(UiText.StringResourceId(R.string.local_tracks_load_error)),
            viewModel.state.value.library,
        )
    }

    private val LocalTracksUiState.isLoadFinished: Boolean
        get() = library is LibraryState.Loaded || library is LibraryState.Failed

    private val LocalTracksUiState.tracks: List<LocalTrack>
        get() = (library as? LibraryState.Loaded)?.tracks.orEmpty()

    private suspend fun ReceiveTurbine<LocalTracksUiState>.awaitLoaded(): LocalTracksUiState =
        awaitState { it.isLoadFinished }

    private suspend fun ReceiveTurbine<LocalTracksUiState>.awaitState(
        predicate: (LocalTracksUiState) -> Boolean,
    ): LocalTracksUiState {
        var state = awaitItem()
        while (!predicate(state)) state = awaitItem()
        return state
    }

    private fun localTrack(id: Long, title: String, artist: String) = LocalTrack(
        id = id,
        title = title,
        artistName = artist,
        duration = 200,
        uri = "content://media/external/audio/media/$id",
    )
}
