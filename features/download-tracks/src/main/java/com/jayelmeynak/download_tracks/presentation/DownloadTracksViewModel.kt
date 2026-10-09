package com.jayelmeynak.download_tracks.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayelmeynak.download_tracks.R
import com.jayelmeynak.lib.designsystem.UiText
import com.jayelmeynak.lib.mediastore.domain.model.LocalTrack
import com.jayelmeynak.lib.mediastore.domain.usecase.GetLocalTracksUseCase
import com.jayelmeynak.lib.mediastore.domain.usecase.GetTrackArtworkUseCase
import com.jayelmeynak.lib.mediastore.domain.usecase.PruneArtworkCacheUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
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

    private var loadJob: Job? = null

    init {
        loadData()
        observeSearchQuery()
    }

    private fun observeSearchQuery() {
        viewModelScope.launch {
            _searchQuery
                // A cleared query goes through at once.
                .debounce { query -> if (query.isBlank()) 0L else SEARCH_DEBOUNCE_MS }
                .distinctUntilChanged()
                .collect { query -> searchTrack(query) }
        }
    }

    private fun searchTrack(query: String) {
        _state.update { it.copy(searchList = it.tracks.search(query)) }
    }

    private fun List<LocalTrack>.search(query: String): List<LocalTrack>? =
        if (query.isBlank()) null else filter { track -> track.matches(query) }

    private fun LocalTrack.matches(query: String): Boolean =
        title.contains(query, ignoreCase = true) || artistName.contains(query, ignoreCase = true)

    private fun loadData() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val tracks = try {
                getLocalTracksUseCase()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(isLoading = false, errorMessage = UiText.StringResourceId(R.string.local_tracks_load_error))
                }
                return@launch
            }
            // A search typed before the reload must show the reloaded tracks.
            _state.update {
                it.copy(
                    tracks = tracks,
                    artworks = emptyMap(),
                    isLoading = false,
                    searchList = tracks.search(_searchQuery.value),
                )
            }

            // Artworks fill in by batches: bounded parallelism and one state update per batch.
            // A broken artwork leaves its track with the placeholder.
            tracks.chunked(ARTWORK_BATCH_SIZE).forEach { batch ->
                val artworks = batch
                    .map { track -> async { track.id to loadArtwork(track) } }
                    .awaitAll()
                _state.update { it.copy(artworks = it.artworks + artworks) }
            }

            try {
                pruneArtworkCacheUseCase(tracks.map { it.id })
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // A stale cache entry is harmless; the list is already shown.
            }
        }
    }

    private suspend fun loadArtwork(track: LocalTrack): ByteArray? = try {
        getTrackArtworkUseCase(track.id, track.uri)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    fun onAction(action: DownloadTracksAction) {
        when (action) {
            is DownloadTracksAction.OnSearchQueryChange -> {
                _state.update { it.copy(query = action.query) }
                _searchQuery.value = action.query
            }

            is DownloadTracksAction.OnAudioPermissionChecked -> onAudioPermissionChecked(action.granted)
        }
    }

    private fun onAudioPermissionChecked(granted: Boolean) {
        val state = _state.value
        when {
            !granted -> {
                loadJob?.cancel()
                _state.update { it.copy(isPermissionDenied = true, isLoading = false) }
            }

            // Reload only when the last load could not have seen the tracks.
            state.isPermissionDenied || state.errorMessage != null -> {
                _state.update { it.copy(isPermissionDenied = false) }
                loadData()
            }
        }
    }
}

private const val ARTWORK_BATCH_SIZE = 20

/** Пауза после ввода, после которой применяется поиск. */
internal const val SEARCH_DEBOUNCE_MS = 500L
