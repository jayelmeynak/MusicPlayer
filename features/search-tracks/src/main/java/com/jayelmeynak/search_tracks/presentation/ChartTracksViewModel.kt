package com.jayelmeynak.search_tracks.presentation

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
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
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChartTracksViewModel @Inject constructor(
    private val getChartUseCase: GetChartUseCase,
    private val searchTrackUseCase: SearchTrackUseCase
) : ViewModel() {

    private val _state = mutableStateOf(ChartTracksState())
    val state: State<ChartTracksState> = _state

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
                _state.value = _state.value.copy(query = action.query)
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
        searchTrackUseCase(query)
            .onSuccess { result ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = null,
                    searchList = result
                )
            }
            .onError { error ->
                _state.value = _state.value.copy(
                    searchList = emptyList(),
                    isLoading = false,
                    errorMessage = error.toUiText()
                )

            }
    }

    private fun getChartList() = viewModelScope.launch {
        _state.value = _state.value.copy(
            isLoading = true
        )
        getChartUseCase()
            .onSuccess { result ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = null,
                    charts = result
                )
            }
            .onError { error ->
                _state.value = _state.value.copy(
                    charts = emptyList(),
                    isLoading = false,
                    errorMessage = error.toUiText()
                )
            }
    }
}