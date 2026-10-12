package com.jayelmeynak.feature.catalog.impl.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayelmeynak.feature.catalog.impl.domain.usecase.GetChartUseCase
import com.jayelmeynak.feature.catalog.impl.domain.usecase.SearchTrackUseCase
import com.jayelmeynak.feature.player.api.PlaybackController
import com.jayelmeynak.lib.designsystem.search.debounceSearchQuery
import com.jayelmeynak.lib.designsystem.toUiText
import com.jayelmeynak.util.result.onError
import com.jayelmeynak.util.result.onSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Экран каталога: чарт, поиск по вводу и запуск воспроизведения. Хранит только исходы загрузок;
 * что показать, вычисляет [CatalogUiState].
 */
@HiltViewModel
internal class CatalogViewModel @Inject constructor(
    private val getChartUseCase: GetChartUseCase,
    private val searchTrackUseCase: SearchTrackUseCase,
    private val playbackController: PlaybackController,
) : ViewModel() {

    private val _state = MutableStateFlow(CatalogUiState())
    val state: StateFlow<CatalogUiState> = _state.asStateFlow()

    private val _effects = Channel<CatalogEffect>(Channel.BUFFERED)
    val effects: Flow<CatalogEffect> = _effects.receiveAsFlow()

    private val searchQueries = MutableStateFlow("")
    private var chartJob: Job? = null

    init {
        loadChart()
        observeSearchQueries()
    }

    fun onAction(action: CatalogAction) {
        when (action) {
            CatalogAction.OnRetryClick -> loadChart()

            is CatalogAction.OnSearchQueryChange -> {
                _state.update { it.copy(query = action.query) }
                searchQueries.value = action.query
            }

            // Очередь — нарисованный список; переход на плеер — после play (docs/playback.md).
            is CatalogAction.OnTrackClick -> {
                playbackController.play(action.tracks.map { it.toQueueItem() }, action.index)
                _effects.trySend(CatalogEffect.OpenPlayer)
            }
        }
    }

    private fun observeSearchQueries() {
        viewModelScope.launch {
            searchQueries
                .debounceSearchQuery()
                .collectLatest { query -> search(query) }
        }
    }

    private suspend fun search(query: String) {
        if (query.isBlank()) {
            _state.update { it.copy(search = SearchState.Inactive) }
            return
        }
        _state.update { it.copy(search = SearchState.Loading) }
        searchTrackUseCase(query)
            .onSuccess { tracks -> _state.update { it.copy(search = SearchState.Loaded(tracks)) } }
            .onError { error -> _state.update { it.copy(search = SearchState.Failed(error.toUiText())) } }
    }

    private fun loadChart() {
        // Повтор отменяет прежнюю загрузку: на экран попадает исход последнего запроса.
        chartJob?.cancel()
        _state.update { it.copy(chart = ChartState.Loading) }
        chartJob = viewModelScope.launch {
            getChartUseCase()
                .onSuccess { tracks -> _state.update { it.copy(chart = ChartState.Loaded(tracks)) } }
                .onError { error -> _state.update { it.copy(chart = ChartState.Failed(error.toUiText())) } }
        }
    }
}
