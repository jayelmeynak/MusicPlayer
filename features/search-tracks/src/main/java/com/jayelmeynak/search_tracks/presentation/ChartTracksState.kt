package com.jayelmeynak.search_tracks.presentation

import com.jayelmeynak.search_tracks.domain.models.Track
import com.jayelmeynak.ui.UiText

data class ChartTracksState(
    val charts: List<Track> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: UiText? = null,
    val query: String = "",
    val searchList: List<Track> = emptyList()
)