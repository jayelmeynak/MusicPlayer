package com.jayelmeynak.feature.localmusic.impl.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayelmeynak.feature.localmusic.impl.R
import com.jayelmeynak.feature.localmusic.impl.domain.model.LocalTrack
import com.jayelmeynak.feature.localmusic.impl.domain.usecase.GetLocalTracksUseCase
import com.jayelmeynak.feature.localmusic.impl.domain.usecase.ObserveLibraryChangesUseCase
import com.jayelmeynak.feature.localmusic.impl.domain.usecase.SearchLocalTracksUseCase
import com.jayelmeynak.feature.player.api.PlaybackController
import com.jayelmeynak.lib.designsystem.UiText
import com.jayelmeynak.lib.designsystem.search.debounceSearchQuery
import com.jayelmeynak.util.result.DataError
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
 * Вкладка «Local»: треки устройства, поиск по вводу, доступ к аудио и запуск воспроизведения.
 * Хранит исходы загрузки и поиска; что показать, вычисляет [LocalTracksUiState].
 *
 * Доступ: решает VM, системные вызовы делает экран по эффектам. Медиатека читается только после
 * первого «доступ есть»; первая проверка без доступа сразу просит системный диалог, дальше — кнопка
 * (диалог или, после окончательного отказа, настройки). С первого доступа и до очистки VM список
 * перечитывается при изменении медиатеки.
 */
@HiltViewModel
internal class LocalTracksViewModel @Inject constructor(
    private val getLocalTracksUseCase: GetLocalTracksUseCase,
    private val searchLocalTracksUseCase: SearchLocalTracksUseCase,
    private val observeLibraryChangesUseCase: ObserveLibraryChangesUseCase,
    private val playbackController: PlaybackController,
) : ViewModel() {

    private val _state = MutableStateFlow(LocalTracksUiState())
    val state: StateFlow<LocalTracksUiState> = _state.asStateFlow()

    private val _effects = Channel<LocalTracksEffect>(Channel.BUFFERED)
    val effects: Flow<LocalTracksEffect> = _effects.receiveAsFlow()

    private val searchQueries = MutableStateFlow("")

    private var loadJob: Job? = null

    private var libraryChangesJob: Job? = null

    /** В этой VM системный запрос уже заканчивался отказом. */
    private var deniedByRequest = false

    init {
        observeSearchQueries()
    }

    fun onAction(action: LocalTracksAction) {
        when (action) {
            is LocalTracksAction.OnSearchQueryChange -> {
                _state.update { it.copy(query = action.query) }
                searchQueries.value = action.query
            }

            is LocalTracksAction.OnAudioPermissionChecked -> onAudioPermissionChecked(action.granted)

            is LocalTracksAction.OnAudioPermissionResult -> onAudioPermissionResult(action)

            LocalTracksAction.OnGrantAccessClick -> {
                val access = _state.value.access
                val effect = if (access is AudioAccess.Denied && access.permanently) {
                    LocalTracksEffect.OpenAppSettings
                } else {
                    LocalTracksEffect.RequestAudioPermission
                }
                _effects.trySend(effect)
            }

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

    /** [showLoading] = false — перезагрузка по изменению медиатеки: прежний список виден до нового. */
    private fun loadTracks(showLoading: Boolean = true) {
        // Перезагрузка отменяет прежнюю: на экран попадает исход последней.
        loadJob?.cancel()
        if (showLoading) _state.update { it.copy(library = LibraryState.Loading) }
        loadJob = viewModelScope.launch {
            getLocalTracksUseCase()
                .onSuccess { tracks -> showTracks(tracks) }
                .onError { error ->
                    _state.update { it.copy(library = LibraryState.Failed(error.toLoadErrorText())) }
                }
        }
    }

    private fun showTracks(tracks: List<LocalTrack>) {
        // Поиск, набранный до перезагрузки, должен показать перезагруженные треки.
        val search = search(tracks, searchQueries.value)
        _state.update { it.copy(library = LibraryState.Loaded(tracks), search = search) }
    }

    private fun onAudioPermissionChecked(granted: Boolean) {
        if (granted) {
            onAccessGranted()
            return
        }
        when (_state.value.access) {
            // Первая проверка без доступа — сразу системный диалог, один раз на жизнь VM.
            AudioAccess.Unknown -> {
                deny(permanently = false)
                _effects.trySend(LocalTracksEffect.RequestAudioPermission)
            }

            AudioAccess.Granted -> deny(permanently = false)

            // Признак окончательного отказа знает только результат запроса — проверка его не сбрасывает.
            is AudioAccess.Denied -> Unit
        }
    }

    /**
     * Окончательный отказ — `rationale == false` после ответа, и при этом отказ уже был: rationale до
     * запроса был `true` или прошлый запрос этой VM кончился отказом. Без второго условия закрытие
     * первого диалога (back, тап мимо — на Android 11+ без rationale) выглядело бы как «больше не
     * спрашивать».
     */
    private fun onAudioPermissionResult(result: LocalTracksAction.OnAudioPermissionResult) {
        if (result.granted) {
            onAccessGranted()
            return
        }
        val permanently = !result.rationaleAfterRequest && (result.rationaleBeforeRequest || deniedByRequest)
        deniedByRequest = true
        deny(permanently)
    }

    private fun deny(permanently: Boolean) {
        loadJob?.cancel()
        _state.update { it.copy(access = AudioAccess.Denied(permanently)) }
    }

    private fun onAccessGranted() {
        val state = _state.value
        observeLibraryChanges()
        // Загрузка — при первом доступе и если прошлая загрузка не могла увидеть треки.
        if (state.access != AudioAccess.Granted || state.library is LibraryState.Failed) {
            // Одним обновлением: экран не мелькает прежним списком между «доступ есть» и загрузкой.
            _state.update { it.copy(access = AudioAccess.Granted, library = LibraryState.Loading) }
            loadTracks()
        }
    }

    /** Одна подписка на жизнь VM: снимается вместе с `viewModelScope`. */
    private fun observeLibraryChanges() {
        if (libraryChangesJob != null) return
        libraryChangesJob = viewModelScope.launch {
            observeLibraryChangesUseCase().collect {
                if (_state.value.access == AudioAccess.Granted) loadTracks(showLoading = false)
            }
        }
    }
}

/** Треки медиатеки; пока она не загружена — пусто. */
private val LocalTracksUiState.libraryTracks: List<LocalTrack>
    get() = (library as? LibraryState.Loaded)?.tracks.orEmpty()

/**
 * Текст ошибки загрузки списка. Отклонение от общего `toUiText()`: тот дал бы «Что-то пошло не так»
 * вместо прежнего «Не удалось загрузить треки», а срез поведение не меняет.
 */
private fun DataError.Local.toLoadErrorText(): UiText = UiText.StringResourceId(R.string.local_tracks_load_error)
