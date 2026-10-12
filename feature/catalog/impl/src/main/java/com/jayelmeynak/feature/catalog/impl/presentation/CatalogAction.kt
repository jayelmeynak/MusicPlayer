package com.jayelmeynak.feature.catalog.impl.presentation

import com.jayelmeynak.feature.catalog.impl.domain.model.Track

internal sealed interface CatalogAction {
    data class OnSearchQueryChange(val query: String) : CatalogAction
    data object OnRetryClick : CatalogAction

    /**
     * Тап по треку [index] в [tracks] — списке, который нарисовал экран (чарт или выдача поиска).
     * Список передаётся, а не берётся из состояния: выдача могла смениться между отрисовкой и тапом.
     */
    data class OnTrackClick(val tracks: List<Track>, val index: Int) : CatalogAction

    /** Тап по иконке «Открыть в Deezer» у трека со ссылкой [link]. */
    data class OnOpenInDeezerClick(val link: String) : CatalogAction
}
