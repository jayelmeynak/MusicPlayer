package com.jayelmeynak.download_tracks.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayelmeynak.local.domain.usecase.GetLocalTracksUseCase
import com.jayelmeynak.local.domain.usecase.GetTrackArtworkUseCase
import com.jayelmeynak.local.domain.usecase.PruneArtworkCacheUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class DownloadTracksViewModel @Inject constructor(
    private val getLocalTracksUseCase: GetLocalTracksUseCase,
    private val getTrackArtworkUseCase: GetTrackArtworkUseCase,
    private val pruneArtworkCacheUseCase: PruneArtworkCacheUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(DownloadTracksState())
    val state: StateFlow<DownloadTracksState> = _state.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    init {
        loadData()
        observeSearchQuery()
    }

    private fun observeSearchQuery() {
        viewModelScope.launch {
            _searchQuery
                .debounce(500)
                .distinctUntilChanged()
                .collect { query -> searchTrack(query) }
        }
    }

    private fun searchTrack(query: String?) = viewModelScope.launch {
        if (query.isNullOrEmpty()) {
            _state.value = _state.value.copy(searchList = emptyList(), query = "")
            return@launch
        }
        val filtered = _state.value.tracks.filter { track ->
            track.title.contains(query, ignoreCase = true) ||
                    track.artistName.contains(query, ignoreCase = true)
        }
        _state.value = _state.value.copy(query = query, searchList = filtered)
    }

    private fun loadData() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val tracks = getLocalTracksUseCase()
            _state.value = _state.value.copy(tracks = tracks)

            pruneArtworkCacheUseCase(tracks.map { it.id })

            val artworks = tracks
                .map { track -> async { track.id to getTrackArtworkUseCase(track.id, track.uri) } }
                .awaitAll()
                .toMap()
            _state.value = _state.value.copy(artworks = artworks, isLoading = false)
        }
    }

    fun onAction(action: DownloadTracksAction) {
        when (action) {
            is DownloadTracksAction.OnTrackClicked -> {}
            is DownloadTracksAction.OnSearchQueryChange -> {
                _state.value = _state.value.copy(query = action.query)
                _searchQuery.value = action.query
            }
        }
    }
}
