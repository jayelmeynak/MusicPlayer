package com.jayelmeynak.search_tracks.presentation

import app.cash.turbine.test
import com.jayelmeynak.util.result.DataError
import com.jayelmeynak.util.result.Result
import com.jayelmeynak.search_tracks.data.toTrack
import com.jayelmeynak.search_tracks.data.trackDto
import com.jayelmeynak.search_tracks.domain.usecase.GetChartUseCase
import com.jayelmeynak.search_tracks.domain.usecase.SearchTrackUseCase
import com.jayelmeynak.lib.designsystem.R
import com.jayelmeynak.lib.designsystem.UiText
import com.jayelmeynak.util.testing.MainDispatcherRule
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

    private val chart = listOf(trackDto().toTrack()!!, trackDto().copy(id = 43L).toTrack()!!)
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
    fun `Deezer не нашёл данные - errorMessage NOT_FOUND, charts пуст`() = runTest {
        repository.chartResult = Result.Error(DataError.Remote.NOT_FOUND)

        viewModel().state.test {
            val state = expectMostRecentItem()
            assertTrue(state.charts.isEmpty())
            assertFalse(state.isLoading)
            assertEquals(R.string.error_not_found, state.errorResId())
        }
    }

    @Test
    fun `квота Deezer на чарте - errorMessage TOO_MANY_REQUESTS`() = runTest {
        repository.chartResult = Result.Error(DataError.Remote.TOO_MANY_REQUESTS)

        viewModel().state.test {
            val state = expectMostRecentItem()
            assertTrue(state.charts.isEmpty())
            assertEquals(R.string.error_too_many_requests, state.errorResId())
        }
    }

    @Test
    fun `ошибка чарта переживает начальный пустой поиск`() = runTest {
        repository.chartResult = Result.Error(DataError.Remote.NO_INTERNET)
        val viewModel = viewModel()

        viewModel.state.test {
            advanceTimeBy(501)
            val state = expectMostRecentItem()
            assertEquals(R.string.error_no_internet, state.errorResId())
            assertFalse(state.isLoading)
            assertTrue(state.charts.isEmpty())
            assertTrue(repository.searchQueries.isEmpty())
        }
    }

    @Test
    fun `ошибка чарта возвращается после очистки строки поиска`() = runTest {
        repository.chartResult = Result.Error(DataError.Remote.NO_INTERNET)
        repository.searchResult = { Result.Success(chart) }
        val viewModel = viewModel()

        viewModel.state.test {
            viewModel.onAction(ChartTracksAction.OnSearchQueryChange("abc"))
            advanceTimeBy(501)
            assertNull(expectMostRecentItem().errorMessage)

            viewModel.onAction(ChartTracksAction.OnSearchQueryChange(""))

            val state = expectMostRecentItem()
            assertEquals(R.string.error_no_internet, state.errorResId())
            assertNull(state.searchList)
        }
    }

    @Test
    fun `поиск без совпадений - пустой результат, а не отсутствие поиска`() = runTest {
        val viewModel = viewModel()

        viewModel.state.test {
            viewModel.onAction(ChartTracksAction.OnSearchQueryChange("zzzzzz"))
            advanceTimeBy(501)

            assertEquals(emptyList<Any>(), expectMostRecentItem().searchList)
        }
    }

    @Test
    fun `очистка строки сразу убирает результат и ошибку поиска`() = runTest {
        repository.searchResult = { Result.Error(DataError.Remote.TOO_MANY_REQUESTS) }
        val viewModel = viewModel()

        viewModel.state.test {
            viewModel.onAction(ChartTracksAction.OnSearchQueryChange("abc"))
            advanceTimeBy(501)
            viewModel.onAction(ChartTracksAction.OnSearchQueryChange(""))

            val state = expectMostRecentItem()
            assertNull(state.searchList)
            assertNull(state.errorMessage)
            assertFalse(state.isLoading)
            assertEquals(chart, state.charts)
        }
    }

    @Test
    fun `медленный ответ на прежний запрос не перетирает результат нового`() = runTest {
        val slow = listOf(trackDto().copy(id = 1L, title = "Slow").toTrack()!!)
        val fresh = listOf(trackDto().copy(id = 2L, title = "Fresh").toTrack()!!)
        repository.searchResult = { q -> Result.Success(if (q == "a") slow else fresh) }
        repository.searchDelayMs = { q -> if (q == "a") 1_000L else 100L }
        val viewModel = viewModel()

        viewModel.state.test {
            viewModel.onAction(ChartTracksAction.OnSearchQueryChange("a"))
            advanceTimeBy(501)
            viewModel.onAction(ChartTracksAction.OnSearchQueryChange("ab"))
            // "ab" is done at ~1101 ms while "a" would still run until ~1500 ms.
            advanceTimeBy(650)

            val state = expectMostRecentItem()
            assertEquals(fresh, state.searchList)
            assertFalse(state.isLoading)

            advanceTimeBy(1_000)
            assertEquals(fresh, viewModel.state.value.searchList)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `очистка во время медленного поиска - поздний ответ не появляется`() = runTest {
        repository.searchResult = { Result.Success(chart) }
        repository.searchDelayMs = { 1_000L }
        val viewModel = viewModel()

        viewModel.state.test {
            viewModel.onAction(ChartTracksAction.OnSearchQueryChange("abc"))
            advanceTimeBy(600)
            viewModel.onAction(ChartTracksAction.OnSearchQueryChange(""))
            advanceTimeBy(2_000)

            val state = expectMostRecentItem()
            assertNull(state.searchList)
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun `очистка и повтор того же запроса внутри паузы - результаты видны`() = runTest {
        repository.searchResult = { Result.Success(chart) }
        val viewModel = viewModel()

        viewModel.state.test {
            viewModel.onAction(ChartTracksAction.OnSearchQueryChange("abc"))
            advanceTimeBy(501)
            viewModel.onAction(ChartTracksAction.OnSearchQueryChange(""))
            advanceTimeBy(100)
            viewModel.onAction(ChartTracksAction.OnSearchQueryChange("abc"))
            advanceTimeBy(501)

            val state = expectMostRecentItem()
            assertEquals("abc", state.query)
            assertEquals(chart, state.searchList)
        }
    }

    @Test
    fun `query обновляется сразу, поиск идёт только после паузы 500 мс`() = runTest {
        val found = listOf(trackDto().copy(id = 1L, title = "Found").toTrack()!!)
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
            assertNull(state.searchList)
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
            assertEquals(emptyList<Any>(), state.searchList)
            assertFalse(state.isLoading)
            assertEquals(R.string.error_too_many_requests, state.errorResId())
            assertEquals(chart, state.charts)
        }
    }

    private fun ChartTracksState.errorResId(): Int? =
        (errorMessage as? UiText.StringResourceId)?.id
}
