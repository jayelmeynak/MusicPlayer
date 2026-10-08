package com.jayelmeynak.download_tracks.presentation

import com.jayelmeynak.lib.mediastore.domain.model.LocalTrack
import com.jayelmeynak.lib.designsystem.UiText

data class DownloadTracksState(
    val tracks: List<LocalTrack> = emptyList(),
    val artworks: Map<Long, ByteArray?> = emptyMap(),
    val isLoading: Boolean = true,
    val errorMessage: UiText? = null,
    val query: String = "",
    /** Null while no search is active; empty when the search found nothing. */
    val searchList: List<LocalTrack>? = null,
    val isPermissionDenied: Boolean = false,
)
