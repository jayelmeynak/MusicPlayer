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
            assertEquals(UiText.StringResourceId(R.string.error_no_internet), state.errorMessage)
        }
    }

    @Test
    fun `Deezer не нашёл данные - errorMessage NOT_FOUND, charts пуст`() = runTest {
        repository.chartResult = Result.Error(DataError.Remote.NOT_FOUND)

        viewModel().state.test {
            val state = expectMostRecentItem()
            assertTrue(state.charts.isEmpty())
            assertFalse(state.isLoading)
            assertEquals(UiText.StringResourceId(R.string.error_not_found), state.errorMessage)
        }
    }

    @Test
    fun `квота Deezer на чарте - errorMessage TOO_MANY_REQUESTS`() = runTest {
        repository.chartResult = Result.Error(DataError.Remote.TOO_MANY_REQUESTS)

        viewModel().state.test {
            val state = expectMostRecentItem()
            assertTrue(state.charts.isEmpty())
            assertEquals(UiText.StringResourceId(R.string.error_too_many_requests), state.errorMessage)
        }
    }

    @Test
    fun `ошибка чарта переживает начальный пустой поиск`() = runTest {
        repository.chartResult = Result.Error(DataError.Remote.NO_INTERNET)
        val viewModel = viewModel()

        viewModel.state.test {
            advanceTimeBy(SEARCH_DEBOUNCE_MS + 1)
            val state = expectMostRecentItem()
            assertEquals(UiText.StringResourceId(R.string.error_no_internet), state.errorMessage)
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
            advanceTimeBy(SEARCH_DEBOUNCE_MS + 1)
            assertNull(expectMostRecentItem().errorMessage)

            viewModel.onAction(ChartTracksAction.OnSearchQueryChange(""))

            val state = expectMostRecentItem()
            assertEquals(UiText.StringResourceId(R.string.error_no_internet), state.errorMessage)
            assertNull(state.searchList)
        }
    }

    @Test
    fun `поиск без совпадений - пустой результат, а не отсутствие поиска`() = runTest {
        val viewModel = viewModel()

        viewModel.state.test {
            viewModel.onAction(ChartTracksAction.OnSearchQueryChange("zzzzzz"))
            advanceTimeBy(SEARCH_DEBOUNCE_MS + 1)

            assertEquals(emptyList<Any>(), expectMostRecentItem().searchList)
        }
    }

    @Test
    fun `очистка строки сразу убирает результат и ошибку поиска`() = runTest {
        repository.searchResult = { Result.Error(DataError.Remote.TOO_MANY_REQUESTS) }
        val viewModel = viewModel()

        viewModel.state.test {
            viewModel.onAction(ChartTracksAction.OnSearchQueryChange("abc"))
            advanceTimeBy(SEARCH_DEBOUNCE_MS + 1)
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
            advanceTimeBy(SEARCH_DEBOUNCE_MS + 1)
            viewModel.onAction(ChartTracksAction.OnSearchQueryChange("ab"))
            // "ab" готов через паузу + 100 мс, а "a" ещё шёл бы до паузы + 1000 мс.
            advanceTimeBy(SEARCH_DEBOUNCE_MS + 150)

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
            advanceTimeBy(SEARCH_DEBOUNCE_MS + 100)
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
            advanceTimeBy(SEARCH_DEBOUNCE_MS + 1)
            viewModel.onAction(ChartTracksAction.OnSearchQueryChange(""))
            advanceTimeBy(100)
            viewModel.onAction(ChartTracksAction.OnSearchQueryChange("abc"))
            advanceTimeBy(SEARCH_DEBOUNCE_MS + 1)

            val state = expectMostRecentItem()
            assertEquals("abc", state.query)
            assertEquals(chart, state.searchList)
        }
    }

    @Test
    fun `query обновляется сразу, поиск идёт только после паузы ввода`() = runTest {
        val found = listOf(trackDto().copy(id = 1L, title = "Found").toTrack()!!)
        repository.searchResult = { Result.Success(found) }
        val viewModel = viewModel()

        viewModel.state.test {
            viewModel.onAction(ChartTracksAction.OnSearchQueryChange("abc"))
            assertEquals("abc", expectMostRecentItem().query)

            advanceTimeBy(SEARCH_DEBOUNCE_MS - 1)
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
            advanceTimeBy(SEARCH_DEBOUNCE_MS + 1)
            viewModel.onAction(ChartTracksAction.OnSearchQueryChange("abcd"))
            advanceTimeBy(100)
            viewModel.onAction(ChartTracksAction.OnSearchQueryChange("abc"))
            advanceTimeBy(SEARCH_DEBOUNCE_MS + 1)

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
            advanceTimeBy(SEARCH_DEBOUNCE_MS + 1)
            viewModel.onAction(ChartTracksAction.OnSearchQueryChange(""))
            advanceTimeBy(SEARCH_DEBOUNCE_MS + 1)

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
            advanceTimeBy(SEARCH_DEBOUNCE_MS + 1)

            val state = expectMostRecentItem()
            assertEquals(emptyList<Any>(), state.searchList)
            assertFalse(state.isLoading)
            assertEquals(UiText.StringResourceId(R.string.error_too_many_requests), state.errorMessage)
            assertEquals(chart, state.charts)
        }
    }

    @Test
    fun `повтор после ошибки чарта - чарт без ошибки`() = runTest {
        repository.chartResult = Result.Error(DataError.Remote.NO_INTERNET)
        val viewModel = viewModel()

        viewModel.state.test {
            assertTrue(expectMostRecentItem().isChartError)

            repository.chartResult = Result.Success(chart)
            viewModel.onAction(ChartTracksAction.OnRetryClick)

            val state = expectMostRecentItem()
            assertEquals(chart, state.charts)
            assertNull(state.errorMessage)
            assertFalse(state.isLoading)
            assertFalse(state.isChartError)
            assertEquals(2, repository.chartRequests)
        }
    }

    @Test
    fun `повтор чарта показывает загрузку до ответа`() = runTest {
        repository.chartResult = Result.Error(DataError.Remote.NO_INTERNET)
        repository.chartDelayMs = { call -> if (call == 1) 0L else 1_000L }
        val viewModel = viewModel()

        viewModel.state.test {
            expectMostRecentItem()
            repository.chartResult = Result.Success(chart)
            viewModel.onAction(ChartTracksAction.OnRetryClick)

            val loading = expectMostRecentItem()
            assertTrue(loading.isLoading)
            assertNull(loading.errorMessage)

            advanceTimeBy(1_001)
            assertEquals(chart, expectMostRecentItem().charts)
        }
    }

    @Test
    fun `повтор чарта снова с ошибкой - ошибка и кнопка повтора`() = runTest {
        repository.chartResult = Result.Error(DataError.Remote.NO_INTERNET)
        val viewModel = viewModel()

        viewModel.state.test {
            expectMostRecentItem()
            repository.chartResult = Result.Error(DataError.Remote.REQUEST_TIMEOUT)
            viewModel.onAction(ChartTracksAction.OnRetryClick)

            val state = expectMostRecentItem()
            assertEquals(UiText.StringResourceId(R.string.error_request_timeout), state.errorMessage)
            assertFalse(state.isLoading)
            assertTrue(state.isChartError)
        }
    }

    @Test
    fun `двойной повтор - на экране исход последнего запроса`() = runTest {
        val stale = listOf(trackDto().copy(id = 7L, title = "Stale").toTrack()!!)
        repository.chartResultFor = { call ->
            when (call) {
                1 -> Result.Error(DataError.Remote.NO_INTERNET)
                2 -> Result.Success(stale)
                else -> Result.Success(chart)
            }
        }
        repository.chartDelayMs = { call -> if (call == 2) 1_000L else 0L }
        val viewModel = viewModel()

        viewModel.state.test {
            viewModel.onAction(ChartTracksAction.OnRetryClick)
            viewModel.onAction(ChartTracksAction.OnRetryClick)
            advanceTimeBy(2_000)

            assertEquals(chart, expectMostRecentItem().charts)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `ошибка поиска - не ошибка чарта, кнопки повтора нет`() = runTest {
        repository.searchResult = { Result.Error(DataError.Remote.TOO_MANY_REQUESTS) }
        val viewModel = viewModel()

        viewModel.state.test {
            viewModel.onAction(ChartTracksAction.OnSearchQueryChange("abc"))
            advanceTimeBy(SEARCH_DEBOUNCE_MS + 1)

            val state = expectMostRecentItem()
            assertEquals(UiText.StringResourceId(R.string.error_too_many_requests), state.errorMessage)
            assertFalse(state.isChartError)
        }
    }

    @Test
    fun `повтор при активном поиске не трогает поиск, после очистки виден новый чарт`() = runTest {
        repository.chartResult = Result.Error(DataError.Remote.NO_INTERNET)
        val found = listOf(trackDto().copy(id = 1L, title = "Found").toTrack()!!)
        repository.searchResult = { Result.Success(found) }
        val viewModel = viewModel()

        viewModel.state.test {
            viewModel.onAction(ChartTracksAction.OnSearchQueryChange("abc"))
            advanceTimeBy(SEARCH_DEBOUNCE_MS + 1)
            repository.chartResult = Result.Success(chart)
            viewModel.onAction(ChartTracksAction.OnRetryClick)

            val searching = expectMostRecentItem()
            assertEquals(found, searching.searchList)
            assertFalse(searching.isLoading)

            viewModel.onAction(ChartTracksAction.OnSearchQueryChange(""))
            val cleared = expectMostRecentItem()
            assertEquals(chart, cleared.charts)
            assertNull(cleared.errorMessage)
        }
    }

    @Test
    fun `пустой чарт без ошибки - пустое состояние`() = runTest {
        repository.chartResult = Result.Success(emptyList())

        viewModel().state.test {
            val state = expectMostRecentItem()
            assertTrue(state.isChartEmpty)
            assertFalse(state.isChartError)
        }
    }

    @Test
    fun `ошибка чарта или непустой чарт - не пустое состояние`() = runTest {
        viewModel().state.test {
            assertFalse(expectMostRecentItem().isChartEmpty)
        }
        repository.chartResult = Result.Error(DataError.Remote.NO_INTERNET)
        viewModel().state.test {
            assertFalse(expectMostRecentItem().isChartEmpty)
        }
    }

    @Test
    fun `введён запрос до срабатывания поиска - ни ошибки чарта, ни пустого чарта`() = runTest {
        repository.chartResult = Result.Error(DataError.Remote.NO_INTERNET)
        val failed = viewModel()
        repository.chartResult = Result.Success(emptyList())
        val empty = viewModel()

        failed.onAction(ChartTracksAction.OnSearchQueryChange("abc"))
        empty.onAction(ChartTracksAction.OnSearchQueryChange("abc"))

        assertFalse(failed.state.value.isChartError)
        assertFalse(empty.state.value.isChartEmpty)
    }
}
