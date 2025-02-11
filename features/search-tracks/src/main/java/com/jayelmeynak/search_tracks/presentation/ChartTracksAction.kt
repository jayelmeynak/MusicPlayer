package com.jayelmeynak.search_tracks.presentation

sealed interface ChartTracksAction {
    data class OnTrackClicked(val id: String): ChartTracksAction
    data class OnSearchQueryChange(val query: String): ChartTracksAction
}