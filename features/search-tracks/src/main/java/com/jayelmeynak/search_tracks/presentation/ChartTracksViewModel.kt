package com.jayelmeynak.search_tracks.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayelmeynak.network.utils.onError
import com.jayelmeynak.network.utils.onSuccess
import com.jayelmeynak.search_tracks.domain.usecase.GetChartUseCase
import com.jayelmeynak.search_tracks.domain.usecase.SearchTrackUseCase
import com.jayelmeynak.ui.toUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChartTracksViewModel @Inject constructor(
    private val getChartUseCase: GetChartUseCase,
    private val searchTrackUseCase: SearchTrackUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ChartTracksState())
    val state: StateFlow<ChartTracksState> = _state.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    init {
        getChartList()
        observeSearchQuery()
    }

    fun onAction(action: ChartTracksAction) {
        when (action) {
            is ChartTracksAction.OnTrackClicked -> {

            }

            is ChartTracksAction.OnSearchQueryChange -> {
                _searchQuery.value = action.query
                _state.update { it.copy(query = action.query) }
            }
        }
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
        _state.update { it.copy(isLoading = true) }
        if (query.isNullOrEmpty()) {
            _state.update {
                it.copy(
                    searchList = emptyList(),
                    isLoading = false,
                    errorMessage = null
                )
            }
            return@launch
        }
        searchTrackUseCase(query)
            .onSuccess { result ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = null,
                        searchList = result
                    )
                }
            }
            .onError { error ->
                _state.update {
                    it.copy(
                        searchList = emptyList(),
                        isLoading = false,
                        errorMessage = error.toUiText()
                    )
                }
            }
    }

    private fun getChartList() = viewModelScope.launch {
        _state.update { it.copy(isLoading = true) }
        getChartUseCase()
            .onSuccess { result ->
                _state.update { it.copy(isLoading = false, errorMessage = null, charts = result) }
            }
            .onError { error ->
                _state.update {
                    it.copy(
                        charts = emptyList(),
                        isLoading = false,
                        errorMessage = error.toUiText()
                    )
                }
            }
    }
}