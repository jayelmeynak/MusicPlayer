package com.jayelmeynak.feature.localmusic.impl.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayelmeynak.feature.localmusic.impl.R
import com.jayelmeynak.feature.localmusic.impl.domain.model.LocalTrack
import com.jayelmeynak.feature.localmusic.impl.domain.usecase.GetLocalTracksUseCase
import com.jayelmeynak.feature.localmusic.impl.domain.usecase.SearchLocalTracksUseCase
import com.jayelmeynak.feature.player.api.PlaybackController
import com.jayelmeynak.lib.designsystem.UiText
import com.jayelmeynak.lib.mediastore.domain.usecase.GetTrackArtworkUseCase
import com.jayelmeynak.lib.mediastore.domain.usecase.PruneArtworkCacheUseCase
import com.jayelmeynak.util.result.DataError
import com.jayelmeynak.util.result.onError
import com.jayelmeynak.util.result.onSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
internal class LocalTracksViewModel @Inject constructor(
    private val getLocalTracksUseCase: GetLocalTracksUseCase,
    private val searchLocalTracksUseCase: SearchLocalTracksUseCase,
    private val getTrackArtworkUseCase: GetTrackArtworkUseCase,
    private val pruneArtworkCacheUseCase: PruneArtworkCacheUseCase,
    private val playbackController: PlaybackController,
) : ViewModel() {

    private val _state = MutableStateFlow(LocalTracksState())
    val state: StateFlow<LocalTracksState> = _state.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    private var loadJob: Job? = null

    init {
        loadData()
        observeSearchQuery()
    }

    private fun observeSearchQuery() {
        viewModelScope.launch {
            _searchQuery
                // Очищенный запрос применяется сразу.
                .debounce { query -> if (query.isBlank()) 0L else SEARCH_DEBOUNCE_MS }
                .distinctUntilChanged()
                .collect { query -> searchTrack(query) }
        }
    }

    private fun searchTrack(query: String) {
        // Выдача считается внутри update: перезагрузка не перетрёт её результатом по старому списку.
        _state.update { it.copy(searchList = search(it.tracks, query)) }
    }

    /** `null` — поиск неактивен (пустой запрос). */
    private fun search(tracks: List<LocalTrack>, query: String): List<LocalTrack>? =
        if (query.isBlank()) null else searchLocalTracksUseCase(tracks, query)

    private fun loadData() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            getLocalTracksUseCase()
                .onSuccess { tracks -> showTracks(tracks) }
                .onError { error ->
                    _state.update { it.copy(isLoading = false, errorMessage = error.toLoadErrorText()) }
                }
        }
    }

    private suspend fun showTracks(tracks: List<LocalTrack>) {
        coroutineScope {
            // Поиск, набранный до перезагрузки, должен показать перезагруженные треки.
            val searchList = search(tracks, _searchQuery.value)
            _state.update {
                it.copy(
                    tracks = tracks,
                    artworks = emptyMap(),
                    isLoading = false,
                    searchList = searchList,
                )
            }

            // Обложки подгружаются пачками: ограниченный параллелизм и одно обновление state на пачку.
            // Сломанная обложка оставляет у трека заглушку.
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
                // Лишняя запись кэша безвредна: список уже показан.
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

    fun onAction(action: LocalTracksAction) {
        when (action) {
            is LocalTracksAction.OnSearchQueryChange -> {
                _state.update { it.copy(query = action.query) }
                _searchQuery.value = action.query
            }

            is LocalTracksAction.OnAudioPermissionChecked -> onAudioPermissionChecked(action.granted)

            // Играет список, который нарисовал экран: выдача могла смениться после отрисовки.
            is LocalTracksAction.OnTrackClick ->
                playbackController.play(action.tracks.map { it.toQueueItem() }, action.index)
        }
    }

    private fun onAudioPermissionChecked(granted: Boolean) {
        val state = _state.value
        when {
            !granted -> {
                loadJob?.cancel()
                _state.update { it.copy(isPermissionDenied = true, isLoading = false) }
            }

            // Перезагрузка — только если прошлая загрузка не могла увидеть треки.
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

/**
 * Текст ошибки загрузки списка. Отклонение от общего `toUiText()`: тот дал бы «Что-то пошло не так»
 * вместо прежнего «Не удалось загрузить треки», а срез поведение не меняет.
 */
private fun DataError.Local.toLoadErrorText(): UiText = UiText.StringResourceId(R.string.local_tracks_load_error)
