package com.jayelmeynak.feature.localmusic.impl.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayelmeynak.feature.localmusic.impl.R
import com.jayelmeynak.feature.localmusic.impl.domain.model.LocalTrack
import com.jayelmeynak.feature.localmusic.impl.domain.usecase.GetLocalTracksUseCase
import com.jayelmeynak.feature.localmusic.impl.domain.usecase.SearchLocalTracksUseCase
import com.jayelmeynak.feature.player.api.PlaybackController
import com.jayelmeynak.lib.designsystem.UiText
import com.jayelmeynak.lib.designsystem.search.debounceSearchQuery
import com.jayelmeynak.lib.mediastore.domain.usecase.GetTrackArtworkUseCase
import com.jayelmeynak.lib.mediastore.domain.usecase.PruneArtworkCacheUseCase
import com.jayelmeynak.util.result.DataError
import com.jayelmeynak.util.result.onError
import com.jayelmeynak.util.result.onSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
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
 * Вкладка «Local»: треки устройства, поиск по вводу, доступ к аудио и запуск воспроизведения.
 * Хранит исходы загрузки и поиска; что показать, вычисляет [LocalTracksUiState].
 */
@HiltViewModel
internal class LocalTracksViewModel @Inject constructor(
    private val getLocalTracksUseCase: GetLocalTracksUseCase,
    private val searchLocalTracksUseCase: SearchLocalTracksUseCase,
    private val getTrackArtworkUseCase: GetTrackArtworkUseCase,
    private val pruneArtworkCacheUseCase: PruneArtworkCacheUseCase,
    private val playbackController: PlaybackController,
) : ViewModel() {

    private val _state = MutableStateFlow(LocalTracksUiState())
    val state: StateFlow<LocalTracksUiState> = _state.asStateFlow()

    private val _effects = Channel<LocalTracksEffect>(Channel.BUFFERED)
    val effects: Flow<LocalTracksEffect> = _effects.receiveAsFlow()

    private val searchQueries = MutableStateFlow("")

    private var loadJob: Job? = null

    init {
        loadTracks()
        observeSearchQueries()
    }

    fun onAction(action: LocalTracksAction) {
        when (action) {
            is LocalTracksAction.OnSearchQueryChange -> {
                _state.update { it.copy(query = action.query) }
                searchQueries.value = action.query
            }

            is LocalTracksAction.OnAudioPermissionChecked -> onAudioPermissionChecked(action.granted)

            // Очередь — нарисованный список; переход на плеер — после play (docs/playback.md).
            is LocalTracksAction.OnTrackClick -> {
                playbackController.play(action.tracks.map { it.toQueueItem() }, action.index)
                _effects.trySend(LocalTracksEffect.OpenPlayer)
            }
        }
    }

    private fun observeSearchQueries() {
        viewModelScope.launch {
            searchQueries
                .debounceSearchQuery()
                .collectLatest { query ->
                    // Выдача считается внутри update: перезагрузка не перетрёт её результатом по старому списку.
                    _state.update { it.copy(search = search(it.libraryTracks, query)) }
                }
        }
    }

    /** `Inactive` — пустой запрос. */
    private fun search(tracks: List<LocalTrack>, query: String): LocalSearchState =
        if (query.isBlank()) LocalSearchState.Inactive
        else LocalSearchState.Results(searchLocalTracksUseCase(tracks, query))

    private fun loadTracks() {
        // Перезагрузка отменяет прежнюю: на экран попадает исход последней.
        loadJob?.cancel()
        _state.update { it.copy(library = LibraryState.Loading) }
        loadJob = viewModelScope.launch {
            getLocalTracksUseCase()
                .onSuccess { tracks -> showTracks(tracks) }
                .onError { error ->
                    _state.update { it.copy(library = LibraryState.Failed(error.toLoadErrorText())) }
                }
        }
    }

    private suspend fun showTracks(tracks: List<LocalTrack>) {
        coroutineScope {
            // Поиск, набранный до перезагрузки, должен показать перезагруженные треки.
            val search = search(tracks, searchQueries.value)
            _state.update {
                it.copy(library = LibraryState.Loaded(tracks), search = search, artworks = emptyMap())
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

    private fun onAudioPermissionChecked(granted: Boolean) {
        val state = _state.value
        when {
            !granted -> {
                loadJob?.cancel()
                _state.update { it.copy(access = AudioAccess.Denied) }
            }

            // Перезагрузка — только если прошлая загрузка не могла увидеть треки.
            state.access == AudioAccess.Denied || state.library is LibraryState.Failed -> {
                // Одним обновлением: экран не мелькает прежним списком между «доступ есть» и загрузкой.
                _state.update { it.copy(access = AudioAccess.Granted, library = LibraryState.Loading) }
                loadTracks()
            }

            else -> _state.update { it.copy(access = AudioAccess.Granted) }
        }
    }
}

private const val ARTWORK_BATCH_SIZE = 20

/** Треки медиатеки; пока она не загружена — пусто. */
private val LocalTracksUiState.libraryTracks: List<LocalTrack>
    get() = (library as? LibraryState.Loaded)?.tracks.orEmpty()

/**
 * Текст ошибки загрузки списка. Отклонение от общего `toUiText()`: тот дал бы «Что-то пошло не так»
 * вместо прежнего «Не удалось загрузить треки», а срез поведение не меняет.
 */
private fun DataError.Local.toLoadErrorText(): UiText = UiText.StringResourceId(R.string.local_tracks_load_error)
