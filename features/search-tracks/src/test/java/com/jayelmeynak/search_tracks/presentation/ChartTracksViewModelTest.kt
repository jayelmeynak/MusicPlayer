package com.jayelmeynak.search_tracks.presentation

import app.cash.turbine.test
import com.jayelmeynak.network.utils.DataError
import com.jayelmeynak.network.utils.Result
import com.jayelmeynak.search_tracks.data.toTrack
import com.jayelmeynak.search_tracks.data.trackDto
import com.jayelmeynak.search_tracks.domain.usecase.GetChartUseCase
import com.jayelmeynak.search_tracks.domain.usecase.SearchTrackUseCase
import com.jayelmeynak.ui.R
import com.jayelmeynak.ui.UiText
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ChartTracksViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val chart = listOf(trackDto().toTrack(), trackDto().copy(id = 43L).toTrack())
    private val repository = FakeMusicChartsRepository(chartResult = Result.Success(chart))

    private fun viewModel() = ChartTracksViewModel(
        GetChartUseCase(repository),
        SearchTrackUseCase(repository),
    )

    @Test
    fun `успешный чарт попадает в charts, загрузка снята`() = runTest {
        viewModel().state.test {
            val state = expectMostRecentItem()
            assertEquals(chart, state.charts)
            assertFalse(state.isLoading)
            assertNull(state.errorMessage)
        }
    }

    @Test
    fun `ошибка чарта попадает в errorMessage, charts пуст`() = runTest {
        repository.chartResult = Result.Error(DataError.Remote.NO_INTERNET)

        viewModel().state.test {
            val state = expectMostRecentItem()
            assertTrue(state.charts.isEmpty())
            assertFalse(state.isLoading)
            assertEquals(R.string.error_no_internet, state.errorResId())
        }
    }

    @Test
    fun `ошибка чарта стирается через 500 мс начальным пустым поиском`() = runTest {
        repository.chartResult = Result.Error(DataError.Remote.NO_INTERNET)
        val viewModel = viewModel()

        viewModel.state.test {
            advanceTimeBy(501)
            val state = expectMostRecentItem()
            assertNull(state.errorMessage)
            assertTrue(state.charts.isEmpty())
            assertTrue(repository.searchQueries.isEmpty())
        }
    }

    @Test
    fun `query обновляется сразу, поиск идёт только после паузы 500 мс`() = runTest {
        val found = listOf(trackDto().copy(id = 1L, title = "Found").toTrack())
        repository.searchResult = { Result.Success(found) }
        val viewModel = viewModel()

        viewModel.state.test {
            viewModel.onAction(ChartTracksAction.OnSearchQueryChange("abc"))
            assertEquals("abc", expectMostRecentItem().query)

            advanceTimeBy(499)
            assertTrue(repository.searchQueries.isEmpty())

            advanceTimeBy(2)
            assertEquals(listOf("abc"), repository.searchQueries)
            val state = expectMostRecentItem()
            assertEquals(found, state.searchList)
            assertFalse(state.isLoading)
            assertNull(state.errorMessage)
        }
    }

    @Test
    fun `возврат к прежнему запросу внутри паузы не ищется повторно`() = runTest {
        val viewModel = viewModel()

        viewModel.state.test {
            viewModel.onAction(ChartTracksAction.OnSearchQueryChange("abc"))
            advanceTimeBy(501)
            viewModel.onAction(ChartTracksAction.OnSearchQueryChange("abcd"))
            advanceTimeBy(100)
            viewModel.onAction(ChartTracksAction.OnSearchQueryChange("abc"))
            advanceTimeBy(501)

            assertEquals(listOf("abc"), repository.searchQueries)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `пустой запрос очищает результаты поиска без обращения к репозиторию`() = runTest {
        repository.searchResult = { Result.Success(chart) }
        val viewModel = viewModel()

        viewModel.state.test {
            viewModel.onAction(ChartTracksAction.OnSearchQueryChange("abc"))
            advanceTimeBy(501)
            viewModel.onAction(ChartTracksAction.OnSearchQueryChange(""))
            advanceTimeBy(501)

            val state = expectMostRecentItem()
            assertTrue(state.searchList.isEmpty())
            assertEquals("", state.query)
            assertEquals(listOf("abc"), repository.searchQueries)
        }
    }

    @Test
    fun `ошибка поиска попадает в errorMessage, searchList пуст, charts сохраняются`() = runTest {
        repository.searchResult = { Result.Error(DataError.Remote.TOO_MANY_REQUESTS) }
        val viewModel = viewModel()

        viewModel.state.test {
            viewModel.onAction(ChartTracksAction.OnSearchQueryChange("abc"))
            advanceTimeBy(501)

            val state = expectMostRecentItem()
            assertTrue(state.searchList.isEmpty())
            assertFalse(state.isLoading)
            assertEquals(R.string.error_too_many_requests, state.errorResId())
            assertEquals(chart, state.charts)
        }
    }

    private fun ChartTracksState.errorResId(): Int? =
        (errorMessage as? UiText.StringResourceId)?.id
}
