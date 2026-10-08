package com.jayelmeynak.download_tracks.presentation

import android.net.Uri
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.jayelmeynak.lib.mediastore.domain.model.LocalTrack
import com.jayelmeynak.lib.mediastore.domain.usecase.GetLocalTracksUseCase
import com.jayelmeynak.lib.mediastore.domain.usecase.GetTrackArtworkUseCase
import com.jayelmeynak.lib.mediastore.domain.usecase.PruneArtworkCacheUseCase
import com.jayelmeynak.util.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
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

// Robolectric only for android.net.Uri inside LocalTrack.
@RunWith(RobolectricTestRunner::class)
class DownloadTracksViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val yesterday = localTrack(1, title = "Yesterday", artist = "The Beatles")
    private val numb = localTrack(2, title = "Numb", artist = "Linkin Park")
    private val help = localTrack(3, title = "Help!", artist = "The Beatles")

    private val repository = FakeLocalTracksRepository(
        tracks = listOf(yesterday, numb, help),
        artworks = mapOf(1L to byteArrayOf(1), 2L to null, 3L to byteArrayOf(3)),
    )

    private fun viewModel() = DownloadTracksViewModel(
        GetLocalTracksUseCase(repository),
        GetTrackArtworkUseCase(repository),
        PruneArtworkCacheUseCase(repository),
    )

    @Test
    fun `загрузка - треки, обложки по id, кэш чистится по актуальным id`() = runTest(ownScheduler()) {
        viewModel().state.test {
            val state = awaitState { !it.isLoading && it.artworks.size == 3 }
            repository.pruned.await()
            assertEquals(listOf(yesterday, numb, help), state.tracks)
            assertEquals(setOf(1L, 2L, 3L), state.artworks.keys)
            assertArrayEquals(byteArrayOf(1), state.artworks[1L])
            assertNull(state.artworks[2L])
            assertEquals(listOf(listOf(1L, 2L, 3L)), repository.prunedIds)
            assertNull(state.errorMessage)
        }
    }

    @Test
    fun `нет треков - пустое состояние, кэш чистится пустым списком`() = runTest(ownScheduler()) {
        repository.tracks = emptyList()

        viewModel().state.test {
            val state = awaitLoaded()
            repository.pruned.await()
            assertTrue(state.tracks.isEmpty())
            assertTrue(state.artworks.isEmpty())
            assertEquals(listOf(emptyList<Long>()), repository.prunedIds)
        }
    }

    @Test
    fun `поиск по исполнителю без учёта регистра после паузы 500 мс`() = runTest(ownScheduler()) {
        val viewModel = viewModel()

        viewModel.state.test {
            awaitLoaded()
            viewModel.onAction(DownloadTracksAction.OnSearchQueryChange("beatles"))
            assertEquals("beatles", expectMostRecentItem().query)

            mainRule.dispatcher.scheduler.advanceTimeBy(499)
            assertNull(viewModel.state.value.searchList)

            mainRule.dispatcher.scheduler.advanceTimeBy(2)
            assertEquals(listOf(yesterday, help), expectMostRecentItem().searchList)
        }
    }

    @Test
    fun `поиск по подстроке названия без учёта регистра`() = runTest(ownScheduler()) {
        val viewModel = viewModel()

        viewModel.state.test {
            awaitLoaded()
            viewModel.onAction(DownloadTracksAction.OnSearchQueryChange("NUM"))
            mainRule.dispatcher.scheduler.advanceTimeBy(501)

            assertEquals(listOf(numb), expectMostRecentItem().searchList)
        }
    }

    @Test
    fun `поиск без совпадений - пустой результат, а не отсутствие поиска`() = runTest(ownScheduler()) {
        val viewModel = viewModel()

        viewModel.state.test {
            awaitLoaded()
            viewModel.onAction(DownloadTracksAction.OnSearchQueryChange("zzzzzz"))
            mainRule.dispatcher.scheduler.advanceTimeBy(501)

            assertEquals(emptyList<LocalTrack>(), expectMostRecentItem().searchList)
        }
    }

    @Test
    fun `очистка строки сразу убирает результат поиска`() = runTest(ownScheduler()) {
        val viewModel = viewModel()

        viewModel.state.test {
            awaitLoaded()
            viewModel.onAction(DownloadTracksAction.OnSearchQueryChange("help"))
            mainRule.dispatcher.scheduler.advanceTimeBy(501)
            viewModel.onAction(DownloadTracksAction.OnSearchQueryChange(""))

            assertNull(expectMostRecentItem().searchList)
        }
    }

    @Test
    fun `список виден до загрузки медленной обложки`() = runTest(ownScheduler()) {
        val slowArtwork = CompletableDeferred<ByteArray?>()
        repository.pendingArtworks[1L] = slowArtwork

        viewModel().state.test {
            val state = awaitLoaded()
            assertEquals(listOf(yesterday, numb, help), state.tracks)
            assertFalse(1L in state.artworks)

            slowArtwork.complete(byteArrayOf(9))
            assertArrayEquals(byteArrayOf(9), awaitState { 1L in it.artworks }.artworks[1L])
        }
    }

    @Test
    fun `исключение при чтении треков - текст ошибки`() = runTest(ownScheduler()) {
        repository.tracksError = SecurityException("no audio permission")

        viewModel().state.test {
            val state = awaitLoaded()
            assertNotNull(state.errorMessage)
            assertTrue(state.tracks.isEmpty())
        }
    }

    @Test
    fun `исключение в обложке одного трека - у него нет обложки, у остальных есть`() = runTest(ownScheduler()) {
        repository.failingArtworks = setOf(1L)

        viewModel().state.test {
            val state = awaitState { !it.isLoading && it.artworks.size == 3 }
            assertNull(state.artworks[1L])
            assertArrayEquals(byteArrayOf(3), state.artworks[3L])
            assertNull(state.errorMessage)
        }
    }

    @Test
    fun `нет разрешения на аудио - состояние без доступа`() = runTest(ownScheduler()) {
        val viewModel = viewModel()

        viewModel.state.test {
            awaitLoaded()
            viewModel.onAction(DownloadTracksAction.OnAudioPermissionChecked(granted = false))

            assertTrue(expectMostRecentItem().isPermissionDenied)
        }
    }

    @Test
    fun `разрешение выдано после отказа - список загружается заново`() = runTest(ownScheduler()) {
        val viewModel = viewModel()

        viewModel.state.test {
            awaitLoaded()
            viewModel.onAction(DownloadTracksAction.OnAudioPermissionChecked(granted = false))
            viewModel.onAction(DownloadTracksAction.OnAudioPermissionChecked(granted = true))

            val state = awaitState { !it.isLoading && !it.isPermissionDenied }
            assertEquals(2, repository.tracksRequests)
            assertEquals(3, state.tracks.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `ошибка загрузки и разрешение есть - список загружается заново`() = runTest(ownScheduler()) {
        repository.tracksError = SecurityException("no audio permission")
        val viewModel = viewModel()

        viewModel.state.test {
            awaitLoaded()
            repository.tracksError = null
            viewModel.onAction(DownloadTracksAction.OnAudioPermissionChecked(granted = true))

            val state = awaitState { !it.isLoading && it.tracks.size == 3 }
            assertFalse(state.isPermissionDenied)
            assertNull(state.errorMessage)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `разрешение есть и список загружен - повторной загрузки нет`() = runTest(ownScheduler()) {
        val viewModel = viewModel()

        viewModel.state.test {
            awaitLoaded()
            viewModel.onAction(DownloadTracksAction.OnAudioPermissionChecked(granted = true))

            assertEquals(1, repository.tracksRequests)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `поиск, набранный до перезагрузки, применяется к новым трекам`() = runTest(ownScheduler()) {
        repository.tracksError = SecurityException("no audio permission")
        val viewModel = viewModel()

        viewModel.state.test {
            awaitLoaded()
            viewModel.onAction(DownloadTracksAction.OnSearchQueryChange("help"))
            mainRule.dispatcher.scheduler.advanceTimeBy(501)
            repository.tracksError = null
            viewModel.onAction(DownloadTracksAction.OnAudioPermissionChecked(granted = true))

            val state = awaitState { !it.isLoading && it.tracks.isNotEmpty() }
            assertEquals(listOf(help), state.searchList)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `очистка и повтор того же запроса внутри паузы - результаты видны`() = runTest(ownScheduler()) {
        val viewModel = viewModel()

        viewModel.state.test {
            awaitLoaded()
            viewModel.onAction(DownloadTracksAction.OnSearchQueryChange("help"))
            mainRule.dispatcher.scheduler.advanceTimeBy(501)
            viewModel.onAction(DownloadTracksAction.OnSearchQueryChange(""))
            mainRule.dispatcher.scheduler.advanceTimeBy(100)
            viewModel.onAction(DownloadTracksAction.OnSearchQueryChange("help"))
            mainRule.dispatcher.scheduler.advanceTimeBy(501)

            assertEquals(listOf(help), expectMostRecentItem().searchList)
        }
    }

    @Test
    fun `пустой запрос очищает searchList`() = runTest(ownScheduler()) {
        val viewModel = viewModel()

        viewModel.state.test {
            awaitLoaded()
            viewModel.onAction(DownloadTracksAction.OnSearchQueryChange("help"))
            mainRule.dispatcher.scheduler.advanceTimeBy(501)
            viewModel.onAction(DownloadTracksAction.OnSearchQueryChange(""))
            mainRule.dispatcher.scheduler.advanceTimeBy(501)

            val state = expectMostRecentItem()
            assertNull(state.searchList)
            assertEquals("", state.query)
        }
    }

    // runTest must not share the Main scheduler: while it waits for the IO hop it would advance
    // virtual time and fire the initial debounce concurrently with loadData. Debounce time is
    // moved explicitly through mainRule. StandardTestDispatcher() without an argument would
    // take the Main scheduler.
    private fun ownScheduler() = StandardTestDispatcher(TestCoroutineScheduler())

    // Loading state comes from Main; the cache prune runs last on the real Dispatchers.IO, so tests
    // that check it await FakeLocalTracksRepository.pruned.
    private suspend fun ReceiveTurbine<DownloadTracksState>.awaitLoaded(): DownloadTracksState =
        awaitState { !it.isLoading }

    private suspend fun ReceiveTurbine<DownloadTracksState>.awaitState(
        predicate: (DownloadTracksState) -> Boolean,
    ): DownloadTracksState {
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
