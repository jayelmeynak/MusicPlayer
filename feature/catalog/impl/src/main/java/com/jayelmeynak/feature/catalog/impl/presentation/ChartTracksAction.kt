package com.jayelmeynak.feature.catalog.impl.presentation

import com.jayelmeynak.feature.catalog.impl.domain.model.Track

internal sealed interface ChartTracksAction {
    data class OnSearchQueryChange(val query: String): ChartTracksAction
    data object OnRetryClick : ChartTracksAction

    /**
     * Тап по треку [index] в [tracks] — списке, который нарисовал экран (чарт или выдача поиска).
     * Список передаётся, а не берётся из state: выдача могла смениться между отрисовкой и тапом.
     */
    data class OnTrackClick(val tracks: List<Track>, val index: Int) : ChartTracksAction
}
