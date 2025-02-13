package com.jayelmeynak.download_tracks.presentation

import com.jayelmeynak.download_tracks.domain.models.Track
import com.jayelmeynak.ui.UiText

data class DownloadTracksState(
    val tracks: List<Track> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: UiText? = null,
    val query: String = "",
    val searchList: List<Track> = emptyList()
)