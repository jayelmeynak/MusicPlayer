package com.jayelmeynak.feature.localmusic.impl.presentation

import android.net.Uri
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.jayelmeynak.feature.localmusic.impl.domain.model.LocalTrack
import com.jayelmeynak.feature.localmusic.impl.domain.usecase.GetLocalTracksUseCase
import com.jayelmeynak.feature.player.api.testing.FakePlaybackController
import com.jayelmeynak.feature.player.api.testing.FakePlaybackController.Command
import com.jayelmeynak.lib.mediastore.domain.usecase.GetTrackArtworkUseCase
import com.jayelmeynak.lib.mediastore.domain.usecase.PruneArtworkCacheUseCase
import com.jayelmeynak.util.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Robolectric — только ради android.net.Uri в LocalTrack.
@RunWith(RobolectricTestRunner::class)
class LocalTracksViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val yesterday = localTrack(1, title = "Yesterday", artist = "The Beatles")
    private val numb = localTrack(2, title = "Numb", artist = "Linkin Park")
    private val help = localTrack(3, title = "Help!", artist = "The Beatles")

    private val repository = FakeLocalTracksRepository(tracks = listOf(yesterday, numb, help))
    private val artworkRepository = FakeArtworkRepository(
        artworks = mapOf(1L to byteArrayOf(1), 2L to null, 3L to byteArrayOf(3)),
    )
    private val playbackController = FakePlaybackController()

    private fun viewModel() = LocalTracksViewModel(
        GetLocalTracksUseCase(repository),
        GetTrackArtworkUseCase(artworkRepository),
        PruneArtworkCacheUseCase(artworkRepository),
        playbackController,
    )

    @Test
    fun `загрузка - треки, обложки по id, кэш чистится по актуальным id`() = runTest {
        viewModel().state.test {
            val state = awaitState { !it.isLoading && it.artworks.size == 3 }
            assertEquals(listOf(yesterday, numb, help), state.tracks)
            assertEquals(setOf(1L, 2L, 3L), state.artworks.keys)
            assertArrayEquals(byteArrayOf(1), state.artworks[1L])
            assertNull(state.artworks[2L])
            assertEquals(listOf(listOf(1L, 2L, 3L)), artworkRepository.prunedIds)
            assertNull(state.errorMessage)
        }
    }

    @Test
    fun `нет треков - пустое состояние, очистка кэша вызвана с пустым списком`() = runTest {
        repository.tracks = emptyList()

        viewModel().state.test {
            val state = awaitLoaded()
            assertTrue(state.tracks.isEmpty())
            assertTrue(state.artworks.isEmpty())
            assertTrue(state.isLibraryEmpty)
            assertEquals(listOf(emptyList<Long>()), artworkRepository.prunedIds)
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
            assertNull(viewModel.state.value.searchList)

            advanceTimeBy(2)
            assertEquals(listOf(yesterday, help), expectMostRecentItem().searchList)
        }
    }

    @Test
    fun `поиск по подстроке названия без учёта регистра`() = runTest {
        val viewModel = viewModel()

        viewModel.state.test {
            awaitLoaded()
            viewModel.onAction(LocalTracksAction.OnSearchQueryChange("NUM"))
            advanceTimeBy(SEARCH_DEBOUNCE_MS + 1)

            assertEquals(listOf(numb), expectMostRecentItem().searchList)
        }
    }

    @Test
    fun `поиск без совпадений - пустой результат, а не отсутствие поиска`() = runTest {
        val viewModel = viewModel()

        viewModel.state.test {
            awaitLoaded()
            viewModel.onAction(LocalTracksAction.OnSearchQueryChange("zzzzzz"))
            advanceTimeBy(SEARCH_DEBOUNCE_MS + 1)

            assertEquals(emptyList<LocalTrack>(), expectMostRecentItem().searchList)
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

            assertNull(expectMostRecentItem().searchList)
        }
    }

    @Test
    fun `список виден до загрузки медленной обложки`() = runTest {
        val slowArtwork = CompletableDeferred<ByteArray?>()
        artworkRepository.pendingArtworks[1L] = slowArtwork

        viewModel().state.test {
            val state = awaitLoaded()
            assertEquals(listOf(yesterday, numb, help), state.tracks)
            assertFalse(1L in state.artworks)

            slowArtwork.complete(byteArrayOf(9))
            assertArrayEquals(byteArrayOf(9), awaitState { 1L in it.artworks }.artworks[1L])
        }
    }

    @Test
    fun `исключение при чтении треков - текст ошибки`() = runTest {
        repository.tracksError = SecurityException("no audio permission")

        viewModel().state.test {
            val state = awaitLoaded()
            assertNotNull(state.errorMessage)
            assertTrue(state.tracks.isEmpty())
        }
    }

    @Test
    fun `исключение в обложке одного трека - у него нет обложки, у остальных есть`() = runTest {
        artworkRepository.failingArtworks = setOf(1L)

        viewModel().state.test {
            val state = awaitState { !it.isLoading && it.artworks.size == 3 }
            assertNull(state.artworks[1L])
            assertArrayEquals(byteArrayOf(3), state.artworks[3L])
            assertNull(state.errorMessage)
        }
    }

    @Test
    fun `нет разрешения на аудио - состояние без доступа`() = runTest {
        val viewModel = viewModel()

        viewModel.state.test {
            awaitLoaded()
            viewModel.onAction(LocalTracksAction.OnAudioPermissionChecked(granted = false))

            assertTrue(expectMostRecentItem().isPermissionDenied)
        }
    }

    @Test
    fun `разрешение выдано после отказа - список загружается заново`() = runTest {
        val viewModel = viewModel()

        viewModel.state.test {
            awaitLoaded()
            viewModel.onAction(LocalTracksAction.OnAudioPermissionChecked(granted = false))
            viewModel.onAction(LocalTracksAction.OnAudioPermissionChecked(granted = true))

            val state = awaitState { !it.isLoading && !it.isPermissionDenied }
            assertEquals(2, repository.tracksRequests)
            assertEquals(3, state.tracks.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `ошибка загрузки и разрешение есть - список загружается заново`() = runTest {
        repository.tracksError = SecurityException("no audio permission")
        val viewModel = viewModel()

        viewModel.state.test {
            awaitLoaded()
            repository.tracksError = null
            viewModel.onAction(LocalTracksAction.OnAudioPermissionChecked(granted = true))

            val state = awaitState { !it.isLoading && it.tracks.size == 3 }
            assertFalse(state.isPermissionDenied)
            assertNull(state.errorMessage)
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
        repository.tracksError = SecurityException("no audio permission")
        val viewModel = viewModel()

        viewModel.state.test {
            awaitLoaded()
            viewModel.onAction(LocalTracksAction.OnSearchQueryChange("help"))
            advanceTimeBy(SEARCH_DEBOUNCE_MS + 1)
            repository.tracksError = null
            viewModel.onAction(LocalTracksAction.OnAudioPermissionChecked(granted = true))

            val state = awaitState { !it.isLoading && it.tracks.isNotEmpty() }
            assertEquals(listOf(help), state.searchList)
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

            assertEquals(listOf(help), expectMostRecentItem().searchList)
        }
    }

    @Test
    fun `пустой запрос очищает searchList`() = runTest {
        val viewModel = viewModel()

        viewModel.state.test {
            awaitLoaded()
            viewModel.onAction(LocalTracksAction.OnSearchQueryChange("help"))
            advanceTimeBy(SEARCH_DEBOUNCE_MS + 1)
            viewModel.onAction(LocalTracksAction.OnSearchQueryChange(""))
            advanceTimeBy(SEARCH_DEBOUNCE_MS + 1)

            val state = expectMostRecentItem()
            assertNull(state.searchList)
            assertEquals("", state.query)
        }
    }

    @Test
    fun `треки есть, ошибка или нет доступа - не пустое состояние`() = runTest {
        val viewModel = viewModel()

        viewModel.state.test {
            assertFalse(awaitLoaded().isLibraryEmpty)
            viewModel.onAction(LocalTracksAction.OnAudioPermissionChecked(granted = false))
            assertFalse(expectMostRecentItem().isLibraryEmpty)
        }
        repository.tracksError = SecurityException("no audio permission")
        viewModel().state.test {
            assertFalse(awaitLoaded().isLibraryEmpty)
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
            val found = expectMostRecentItem().searchList!!
            viewModel.onAction(LocalTracksAction.OnTrackClick(found, 1))

            assertEquals(
                listOf(Command.Play(listOf(yesterday, help).map { it.toQueueItem() }, 1)),
                playbackController.commands,
            )
        }
    }

    private suspend fun ReceiveTurbine<LocalTracksState>.awaitLoaded(): LocalTracksState =
        awaitState { !it.isLoading }

    private suspend fun ReceiveTurbine<LocalTracksState>.awaitState(
        predicate: (LocalTracksState) -> Boolean,
    ): LocalTracksState {
        var state = awaitItem()
        while (!predicate(state)) state = awaitItem()
        return state
    }

    private fun localTrack(id: Long, title: String, artist: String) = LocalTrack(
        id = id,
        title = title,
        artistName = artist,
        duration = 200,
        uri = Uri.parse("content://media/external/audio/media/$id"),
    )
}
