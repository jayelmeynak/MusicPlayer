package com.jayelmeynak.search_tracks.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayelmeynak.util.result.onError
import com.jayelmeynak.util.result.onSuccess
import com.jayelmeynak.search_tracks.domain.usecase.GetChartUseCase
import com.jayelmeynak.search_tracks.domain.usecase.SearchTrackUseCase
import com.jayelmeynak.lib.designsystem.UiText
import com.jayelmeynak.lib.designsystem.toUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
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

    // The chart outcome, restored when the search is cleared: search shares isLoading/errorMessage.
    private var chartLoading = true
    private var chartError: UiText? = null

    init {
        getChartList()
        observeSearchQuery()
    }

    fun onAction(action: ChartTracksAction) {
        when (action) {
            is ChartTracksAction.OnTrackClicked -> {

            }

            is ChartTracksAction.OnSearchQueryChange -> {
                // State first: the search flow reads the query to tell an active search.
                _state.update { it.copy(query = action.query) }
                _searchQuery.value = action.query
            }
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeSearchQuery() {
        viewModelScope.launch {
            _searchQuery
                // A cleared query goes through at once: it cancels a running search right away.
                .debounce { query -> if (query.isBlank()) 0L else 500L }
                .distinctUntilChanged()
                .collectLatest { query -> searchTrack(query) }
        }
    }

    private suspend fun searchTrack(query: String) {
        if (query.isBlank()) {
            clearSearch()
            return
        }
        _state.update { it.copy(isLoading = true) }
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

    private fun clearSearch() {
        _state.update { it.copy(searchList = null).withChartOutcome() }
    }

    /** Loading and error belong to an active search; without one they show the chart outcome. */
    private fun ChartTracksState.withChartOutcome(): ChartTracksState =
        if (query.isNotBlank()) this else copy(isLoading = chartLoading, errorMessage = chartError)

    private fun getChartList() = viewModelScope.launch {
        _state.update { it.copy(isLoading = true) }
        getChartUseCase()
            .onSuccess { result ->
                chartLoading = false
                chartError = null
                _state.update { it.copy(charts = result).withChartOutcome() }
            }
            .onError { error ->
                chartLoading = false
                chartError = error.toUiText()
                _state.update { it.copy(charts = emptyList()).withChartOutcome() }
            }
    }
}
