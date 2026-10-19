package com.jayelmeynak.feature.player.impl.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayelmeynak.feature.player.api.PlaybackController
import com.jayelmeynak.feature.player.api.PlaybackError
import com.jayelmeynak.feature.player.api.PlaybackState
import com.jayelmeynak.feature.player.api.TrackSource
import com.jayelmeynak.feature.player.api.key
import com.jayelmeynak.feature.player.api.positionForPercent
import com.jayelmeynak.feature.player.impl.R
import com.jayelmeynak.feature.player.impl.permission.NotificationPromptStore
import com.jayelmeynak.lib.designsystem.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Экран плеера поверх [PlaybackController]: показывает текущий элемент и шлёт транспортные
 * команды. Скоуп — NavEntry экрана; само состояние воспроизведения живёт в сессии, поэтому новый
 * экземпляр сразу показывает текущий трек.
 */
@HiltViewModel
internal class PlayerViewModel @Inject constructor(
    private val playbackController: PlaybackController,
    private val notificationPromptStore: NotificationPromptStore,
) : ViewModel() {

    private val _state = MutableStateFlow<PlayerUiState>(PlayerUiState.Connecting)
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private val _effects = Channel<PlayerEffect>(Channel.BUFFERED)
    val effects: Flow<PlayerEffect> = _effects.receiveAsFlow()

    private var closeSent = false

    /** Проверка разрешения уведомлений уже обработана: поворот до ответа хранилища не даст второй запрос. */
    private var notificationPromptHandled = false

    init {
        viewModelScope.launch {
            playbackController.state.collect(::onPlaybackState)
        }
        viewModelScope.launch {
            playbackController.positionMs.collect { position ->
                _state.update { state ->
                    if (state is PlayerUiState.Ready) state.copy(positionMs = position) else state
                }
            }
        }
    }

    fun onAction(action: PlayerAction) {
        when (action) {
            PlayerAction.TogglePlayPause -> playbackController.togglePlayPause()
            PlayerAction.Next -> playbackController.next()
            PlayerAction.Previous -> playbackController.previous()
            PlayerAction.SeekBack -> playbackController.seekBack()
            PlayerAction.SeekForward -> playbackController.seekForward()
            is PlayerAction.SeekTo -> seekTo(action.percent)
            is PlayerAction.OnNotificationPermissionChecked -> onNotificationPermissionChecked(action.granted)
        }
    }

    /**
     * Уведомление плеера на API 33+ без разрешения не видно, но воспроизведение работает; поэтому
     * просим один раз на установку и отказ не переспрашиваем. Флаг ставится до диалога.
     */
    private fun onNotificationPermissionChecked(granted: Boolean) {
        if (granted || notificationPromptHandled) return
        notificationPromptHandled = true
        viewModelScope.launch {
            if (notificationPromptStore.isPrompted()) return@launch
            notificationPromptStore.markPrompted()
            _effects.send(PlayerEffect.RequestNotificationPermission)
        }
    }

    private fun seekTo(percent: Float) {
        val duration = (_state.value as? PlayerUiState.Ready)?.durationMs ?: return
        positionForPercent(percent, duration)?.let(playbackController::seekTo)
    }

    private fun onPlaybackState(playback: PlaybackState) {
        val active = when (playback) {
            PlaybackState.Idle -> {
                _state.value = PlayerUiState.Connecting
                return
            }
            PlaybackState.ConnectionFailed -> {
                _state.value = PlayerUiState.ConnectionFailed
                return
            }
            is PlaybackState.Active -> playback
        }
        if (active.queue.isEmpty()) {
            _state.value = PlayerUiState.Connecting
            // Пустой снапшот без подключения ничего не значит: контроллер публикует подключённый
            // снапшот только после восстановления сессии, закрываться можно лишь по нему.
            if (active.isConnected && !closeSent) {
                closeSent = true
                _effects.trySend(PlayerEffect.Close)
            }
            return
        }
        // У чужого текущего элемента (добавлен другим контроллером) нет наших метаданных: показать
        // кнопки без данных трека, а не бесконечный индикатор прогресса.
        val current = active.current
        _state.value = PlayerUiState.Ready(
            trackKey = current?.key ?: "foreign|${active.currentIndex}",
            title = current?.title.orEmpty(),
            artist = current?.artist.orEmpty(),
            artworkUri = current?.artworkUri,
            isPreview = current?.source == TrackSource.DEEZER,
            trackLengthMs = current?.durationMs ?: 0L,
            showPlayButton = active.showPlayButton,
            isBuffering = active.isBuffering,
            durationMs = active.durationMs,
            positionMs = playbackController.positionMs.value,
            errorMessage = active.error?.toUiText(),
        )
    }

    private fun PlaybackError.toUiText(): UiText = UiText.StringResourceId(
        when (this) {
            PlaybackError.SOURCE_UNAVAILABLE -> R.string.player_error_source_unavailable
            PlaybackError.NETWORK -> R.string.player_error_network
            PlaybackError.UNKNOWN -> R.string.player_error_unknown
        }
    )
}
