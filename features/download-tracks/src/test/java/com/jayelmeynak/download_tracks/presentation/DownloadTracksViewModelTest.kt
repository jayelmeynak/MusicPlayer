package com.jayelmeynak.download_tracks.presentation

import android.net.Uri
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.jayelmeynak.local.domain.model.LocalTrack
import com.jayelmeynak.local.domain.usecase.GetLocalTracksUseCase
import com.jayelmeynak.local.domain.usecase.GetTrackArtworkUseCase
import com.jayelmeynak.local.domain.usecase.PruneArtworkCacheUseCase
import kotlinx.coroutines.test.advanceTimeBy
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
    fun `загрузка - треки, обложки по id, кэш чистится по актуальным id`() = runTest {
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
    fun `нет треков - пустое состояние, кэш чистится пустым списком`() = runTest {
        repository.tracks = emptyList()

        viewModel().state.test {
            val state = awaitLoaded()
            assertTrue(state.tracks.isEmpty())
            assertTrue(state.artworks.isEmpty())
            assertEquals(listOf(emptyList<Long>()), repository.prunedIds)
        }
    }

    @Test
    fun `поиск по исполнителю без учёта регистра после паузы 500 мс`() = runTest {
        val viewModel = viewModel()

        viewModel.state.test {
            awaitLoaded()
            viewModel.onAction(DownloadTracksAction.OnSearchQueryChange("beatles"))
            assertEquals("beatles", expectMostRecentItem().query)

            advanceTimeBy(499)
            assertTrue(viewModel.state.value.searchList.isEmpty())

            advanceTimeBy(2)
            assertEquals(listOf(yesterday, help), expectMostRecentItem().searchList)
        }
    }

    @Test
    fun `поиск по подстроке названия без учёта регистра`() = runTest {
        val viewModel = viewModel()

        viewModel.state.test {
            awaitLoaded()
            viewModel.onAction(DownloadTracksAction.OnSearchQueryChange("NUM"))
            advanceTimeBy(501)

            assertEquals(listOf(numb), expectMostRecentItem().searchList)
        }
    }

    @Test
    fun `пустой запрос очищает searchList`() = runTest {
        val viewModel = viewModel()

        viewModel.state.test {
            awaitLoaded()
            viewModel.onAction(DownloadTracksAction.OnSearchQueryChange("help"))
            advanceTimeBy(501)
            viewModel.onAction(DownloadTracksAction.OnSearchQueryChange(""))
            advanceTimeBy(501)

            val state = expectMostRecentItem()
            assertTrue(state.searchList.isEmpty())
            assertEquals("", state.query)
        }
    }

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
