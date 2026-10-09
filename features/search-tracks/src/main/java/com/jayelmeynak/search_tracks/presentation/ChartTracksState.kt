package com.jayelmeynak.search_tracks.presentation

import com.jayelmeynak.search_tracks.domain.models.Track
import com.jayelmeynak.lib.designsystem.UiText

data class ChartTracksState(
    val charts: List<Track> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: UiText? = null,
    val query: String = "",
    /** Null while no search is active; empty when the search found nothing. */
    val searchList: List<Track>? = null
) {
    /** Ошибка без поиска (и без введённого запроса) — ошибка загрузки чарта: экран предлагает повтор. */
    val isChartError: Boolean
        get() = errorMessage != null && searchList == null && query.isBlank()

    /** Чарт загружен без ошибки, пуст, запрос не введён. */
    val isChartEmpty: Boolean
        get() = !isLoading && errorMessage == null && searchList == null && query.isBlank() && charts.isEmpty()
}