package com.jayelmeynak.feature.catalog.impl.presentation

import com.jayelmeynak.feature.catalog.impl.domain.model.Track
import com.jayelmeynak.lib.designsystem.UiText

/**
 * Состояние экрана каталога: введённый запрос и исходы двух загрузок — чарта и поиска. Что
 * показать, вычисляется из них ([content]), а не хранится отдельными флагами.
 */
internal data class CatalogUiState(
    val query: String = "",
    val chart: ChartState = ChartState.Loading,
    val search: SearchState = SearchState.Inactive,
) {
    /**
     * Без запроса или пока поиск по нему ещё не запускался (пауза ввода) — исход чарта; повтор и
     * «Чарт пуст» — только без запроса. Иначе — исход поиска.
     */
    val content: CatalogContent
        get() = if (query.isBlank() || search == SearchState.Inactive) chartContent() else searchContent()

    private fun chartContent(): CatalogContent = when (chart) {
        ChartState.Loading -> CatalogContent.Loading
        is ChartState.Failed -> CatalogContent.Error(chart.message, canRetry = query.isBlank())
        is ChartState.Loaded ->
            if (chart.tracks.isEmpty() && query.isBlank()) CatalogContent.ChartEmpty
            else CatalogContent.Tracks(chart.tracks)
    }

    private fun searchContent(): CatalogContent = when (search) {
        SearchState.Inactive, SearchState.Loading -> CatalogContent.Loading
        is SearchState.Failed -> CatalogContent.Error(search.message, canRetry = false)
        is SearchState.Loaded ->
            if (search.tracks.isEmpty()) CatalogContent.NothingFound else CatalogContent.Tracks(search.tracks)
    }
}

/** Исход загрузки чарта. */
internal sealed interface ChartState {
    data object Loading : ChartState
    data class Loaded(val tracks: List<Track>) : ChartState
    data class Failed(val message: UiText) : ChartState
}

/** Исход поиска по введённому запросу; `Inactive` — поиск по текущему запросу не запускался. */
internal sealed interface SearchState {
    data object Inactive : SearchState
    data object Loading : SearchState
    data class Loaded(val tracks: List<Track>) : SearchState
    data class Failed(val message: UiText) : SearchState
}

/** Что показывает экран каталога. */
internal sealed interface CatalogContent {
    data object Loading : CatalogContent
    /** [canRetry] — ошибка чарта без запроса: экран предлагает повтор. */
    data class Error(val message: UiText, val canRetry: Boolean) : CatalogContent
    data object NothingFound : CatalogContent
    data object ChartEmpty : CatalogContent
    data class Tracks(val tracks: List<Track>) : CatalogContent
}
