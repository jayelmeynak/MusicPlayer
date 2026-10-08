package com.jayelmeynak.download_tracks.presentation

import android.net.Uri
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.jayelmeynak.local.domain.model.LocalTrack
import com.jayelmeynak.local.domain.usecase.GetLocalTracksUseCase
import com.jayelmeynak.local.domain.usecase.GetTrackArtworkUseCase
import com.jayelmeynak.local.domain.usecase.PruneArtworkCacheUseCase
import com.jayelmeynak.util.testing.MainDispatcherRule
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
            val state = awaitLoaded()
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
            assertTrue(viewModel.state.value.searchList.isEmpty())

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
    fun `пустой запрос очищает searchList`() = runTest(ownScheduler()) {
        val viewModel = viewModel()

        viewModel.state.test {
            awaitLoaded()
            viewModel.onAction(DownloadTracksAction.OnSearchQueryChange("help"))
            mainRule.dispatcher.scheduler.advanceTimeBy(501)
            viewModel.onAction(DownloadTracksAction.OnSearchQueryChange(""))
            mainRule.dispatcher.scheduler.advanceTimeBy(501)

            val state = expectMostRecentItem()
            assertTrue(state.searchList.isEmpty())
            assertEquals("", state.query)
        }
    }

    // runTest must not share the Main scheduler: while it waits for the IO hop it would advance
    // virtual time and fire the initial debounce concurrently with loadData. Debounce time is
    // moved explicitly through mainRule. StandardTestDispatcher() without an argument would
    // take the Main scheduler.
    private fun ownScheduler() = StandardTestDispatcher(TestCoroutineScheduler())

    // PruneArtworkCacheUseCase hops to the real Dispatchers.IO, so wait for the loaded state.
    private suspend fun ReceiveTurbine<DownloadTracksState>.awaitLoaded(): DownloadTracksState {
        var state = awaitItem()
        while (state.isLoading) state = awaitItem()
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
