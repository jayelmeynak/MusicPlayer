package com.jayelmeynak.search_tracks.presentation

sealed interface ChartTracksAction {
    data class OnSearchQueryChange(val query: String): ChartTracksAction
    data object OnRetryClick : ChartTracksAction
}
