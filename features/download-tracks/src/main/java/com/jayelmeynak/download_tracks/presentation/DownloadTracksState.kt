package com.jayelmeynak.download_tracks.presentation

import com.jayelmeynak.local.domain.model.LocalTrack
import com.jayelmeynak.ui.UiText

data class DownloadTracksState(
    val tracks: List<LocalTrack> = emptyList(),
    val artworks: Map<Long, ByteArray?> = emptyMap(),
    val isLoading: Boolean = true,
    val errorMessage: UiText? = null,
    val query: String = "",
    val searchList: List<LocalTrack> = emptyList()
)
