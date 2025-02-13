package com.jayelmeynak.download_tracks.presentation

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayelmeynak.download_tracks.domain.MusicLocalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
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
    private val musicLocalRepository: MusicLocalRepository,
) : ViewModel() {

    val _state = mutableStateOf(DownloadTracksState())
    val state: State<DownloadTracksState> = _state

    private val _searchQuery = MutableStateFlow("")

    init {
        loadData()
        observeSearchQuery()
    }

    @OptIn(FlowPreview::class)
    private fun observeSearchQuery() {
        viewModelScope.launch {
            _searchQuery
                .debounce(500)
                .distinctUntilChanged()
                .collect { query ->
                    searchTrack(query)
                }
        }
    }

    private fun searchTrack(query: String?) = viewModelScope.launch {
        _state.value = _state.value.copy(
            isLoading = true
        )
        if (query.isNullOrEmpty()) {
            _state.value = _state.value.copy(
                searchList = emptyList(),
                isLoading = false,
                errorMessage = null
            )
            return@launch
        }
        val filtered = if (query.isBlank()) {
            emptyList()
        } else {
            _state.value.tracks.filter { track ->
                track.title.contains(query, ignoreCase = true) ||
                        track.artistName.contains(query, ignoreCase = true)
            }
        }
        _state.value = _state.value.copy(isLoading= false, query = query, searchList = filtered)

    }

    private fun loadData(){
        viewModelScope.launch {
            _state.value = state.value.copy(isLoading = true)
            _state.value = state.value.copy(tracks = musicLocalRepository.getAudioData())
            _state.value = state.value.copy(isLoading = false)
        }
    }


    fun onAction(action: DownloadTracksAction) {
        when (action) {
            is DownloadTracksAction.OnTrackClicked -> {
                // Handle track click
            }
            is DownloadTracksAction.OnSearchQueryChange -> {
                _state.value = _state.value.copy(query = action.query)
                _searchQuery.value = action.query
            }
        }
    }
}