package com.jayelmeynak.download_tracks.presentation

sealed interface DownloadTracksAction {
    data class OnTrackClicked(val id: String): DownloadTracksAction
    data class OnSearchQueryChange(val query: String): DownloadTracksAction
}