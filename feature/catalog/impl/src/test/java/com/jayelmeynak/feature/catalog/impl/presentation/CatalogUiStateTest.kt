package com.jayelmeynak.feature.catalog.impl.presentation

import com.jayelmeynak.feature.catalog.impl.domain.model.Track
import com.jayelmeynak.lib.designsystem.R
import com.jayelmeynak.lib.designsystem.UiText
import org.junit.Assert.assertEquals
import org.junit.Test

/** Что показывает экран каталога при каждом сочетании запроса, исхода чарта и исхода поиска. */
class CatalogUiStateTest {

    private val chartTracks = listOf(track(1), track(2))
    private val found = listOf(track(3))
    private val chartError = UiText.StringResourceId(R.string.error_no_internet)
    private val searchError = UiText.StringResourceId(R.string.error_too_many_requests)

    @Test
    fun `без запроса - исход чарта`() {
        assertEquals(CatalogContent.Loading, state(chart = ChartState.Loading).content)
        assertEquals(CatalogContent.Tracks(chartTracks), state(chart = ChartState.Loaded(chartTracks)).content)
        assertEquals(CatalogContent.ChartEmpty, state(chart = ChartState.Loaded(emptyList())).content)
    }

    @Test
    fun `ошибка чарта без запроса - с повтором`() {
        assertEquals(
            CatalogContent.Error(chartError, canRetry = true),
            state(chart = ChartState.Failed(chartError)).content,
        )
    }

    @Test
    fun `пустой запрос показывает чарт, даже если исход поиска ещё не сброшен`() {
        val content = state(query = "", chart = ChartState.Loaded(chartTracks), search = SearchState.Loaded(found)).content

        assertEquals(CatalogContent.Tracks(chartTracks), content)
    }

    @Test
    fun `запрос ждёт паузы ввода - текущий исход чарта, без повтора и без пустого чарта`() {
        assertEquals(CatalogContent.Loading, state(query = "a", chart = ChartState.Loading).content)
        assertEquals(
            CatalogContent.Error(chartError, canRetry = false),
            state(query = "a", chart = ChartState.Failed(chartError)).content,
        )
        assertEquals(
            CatalogContent.Tracks(emptyList()),
            state(query = "a", chart = ChartState.Loaded(emptyList())).content,
        )
        assertEquals(
            CatalogContent.Tracks(chartTracks),
            state(query = "a", chart = ChartState.Loaded(chartTracks)).content,
        )
    }

    @Test
    fun `активный поиск - исход поиска поверх любого исхода чарта`() {
        val chart = ChartState.Failed(chartError)

        assertEquals(CatalogContent.Loading, state(query = "a", chart = chart, search = SearchState.Loading).content)
        assertEquals(
            CatalogContent.Error(searchError, canRetry = false),
            state(query = "a", chart = chart, search = SearchState.Failed(searchError)).content,
        )
        assertEquals(
            CatalogContent.NothingFound,
            state(query = "a", chart = chart, search = SearchState.Loaded(emptyList())).content,
        )
        assertEquals(
            CatalogContent.Tracks(found),
            state(query = "a", chart = chart, search = SearchState.Loaded(found)).content,
        )
    }

    private fun state(
        query: String = "",
        chart: ChartState = ChartState.Loaded(chartTracks),
        search: SearchState = SearchState.Inactive,
    ) = CatalogUiState(query = query, chart = chart, search = search)

    private fun track(id: Long) = Track(id = id, title = "T$id", artistName = "A", coverUrl = "", duration = 0L, link = null)
}
