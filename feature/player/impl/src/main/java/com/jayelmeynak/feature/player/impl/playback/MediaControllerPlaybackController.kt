package com.jayelmeynak.feature.player.impl.playback

import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.annotation.MainThread
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.Player
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.session.MediaController
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.jayelmeynak.feature.player.api.PlaybackController
import com.jayelmeynak.feature.player.api.PlaybackState
import com.jayelmeynak.feature.player.api.QueueItem
import com.jayelmeynak.feature.player.impl.service.EXTRA_SESSION_RESTORING
import com.jayelmeynak.feature.player.impl.service.toMediaItem
import com.jayelmeynak.util.coroutines.ApplicationScope
import com.jayelmeynak.util.coroutines.Dispatcher
import com.jayelmeynak.util.coroutines.MusicPlayerDispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import javax.inject.Inject
import javax.inject.Singleton

/** Сколько раз подряд контроллер пробует подключиться к сессии, прежде чем сдаться. */
internal const val MAX_CONNECT_ATTEMPTS = 4

/** Пауза перед второй попыткой подключения; дальше она удваивается (0,5 / 1 / 2 с). */
internal const val CONNECT_RETRY_BASE_MS = 500L

/**
 * [PlaybackController] over a [MediaController] of the playback session: one controller per
 * process, connected while the process lifecycle is started (an Activity is visible) and released
 * on stop, so the session's service can stop in the background. A rotation does not reconnect.
 *
 * The state is a snapshot of the session player rebuilt on every player event: after a reconnect
 * the UI gets the current track, queue and position without any restore code. The position is
 * polled only while playing. All calls come on the main thread, like the controller's callbacks;
 * the first injection must happen on the main thread too, since it observes the process lifecycle.
 *
 * Без подключения (фон, переподключение) [state] держит последний снапшот с `isConnected = false`:
 * мини-плеер и экран не мигают пустотой. Сессия, которая ещё поднимает сохранённую очередь
 * ([EXTRA_SESSION_RESTORING]), снапшот не публикует — пустая очередь в ней ничего не значит; исключение
 * — очередь, которую контроллер задал сам ([play]). Транспортные команды в это время уже уходят в
 * сессию: play на пустом плеере возобновит ту же сохранённую очередь (`onPlaybackResumption`), а
 * восстановление не перетирает непустой плеер. Неудачное подключение повторяется с паузой
 * [CONNECT_RETRY_BASE_MS], удваивая её, до [MAX_CONNECT_ATTEMPTS] попыток, потом —
 * [PlaybackState.ConnectionFailed]; следующий выход на передний план или [play] начинает заново.
 *
 * Public only for the binding in `:di`; nothing outside the module can create it.
 */
@Singleton
@MainThread
@OptIn(UnstableApi::class)
public class MediaControllerPlaybackController @Inject internal constructor(
    @ProcessLifecycle private val lifecycleOwner: LifecycleOwner,
    private val connector: MediaControllerConnector,
    @ApplicationScope private val scope: CoroutineScope,
    @Dispatcher(MusicPlayerDispatchers.Main) private val mainDispatcher: CoroutineDispatcher,
) : PlaybackController {

    private val _state = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    override val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    override val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var pendingPlay: Play? = null
    private var queue: List<QueueItem?> = emptyList()
    private var positionJob: Job? = null
    private var retryJob: Job? = null
    private var attempts = 0

    /** Сессия закончила восстановление (или контроллер сам задал очередь): снапшот можно показывать. */
    private var sessionReady = false

    private val processObserver = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) = connect()
        override fun onStop(owner: LifecycleOwner) = disconnect()
    }

    /** The session went away (e.g. its service was destroyed): reconnect while the app is visible. */
    private val controllerListener = object : MediaController.Listener {
        override fun onDisconnected(controller: MediaController) {
            if (controller !== this@MediaControllerPlaybackController.controller) return
            disconnect()
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) connect()
        }

        override fun onExtrasChanged(controller: MediaController, extras: Bundle) {
            if (controller !== this@MediaControllerPlaybackController.controller) return
            if (sessionReady || extras.isRestoring()) return
            sessionReady = true
            queue = controller.readQueue()
            publish(controller)
            _positionMs.value = controller.currentPosition
            updatePositionPolling(controller)
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (events.contains(Player.EVENT_TIMELINE_CHANGED)) queue = player.readQueue()
            publish(player)
            if (sessionReady && events.containsAny(
                    Player.EVENT_POSITION_DISCONTINUITY,
                    Player.EVENT_MEDIA_ITEM_TRANSITION,
                    Player.EVENT_TIMELINE_CHANGED,
                )
            ) {
                _positionMs.value = player.currentPosition
            }
            if (sessionReady && events.contains(Player.EVENT_IS_PLAYING_CHANGED)) updatePositionPolling(player)
        }
    }

    init {
        // Подписка на процессный LifecycleOwner возможна только на главном потоке: падаем здесь с
        // понятным текстом, а не в библиотеке жизненного цикла.
        check(Looper.myLooper() == Looper.getMainLooper()) {
            "PlaybackController создаётся только на главном потоке: первая инъекция — из Activity или другого кода на main"
        }
        lifecycleOwner.lifecycle.addObserver(processObserver)
    }

    /**
     * Replaces the queue and plays [startIndex] from the start. The same queue with the same
     * current item keeps playing from where it is (a paused or stopped one resumes), so tapping
     * the playing track in a list neither restarts nor pauses it.
     */
    override fun play(queue: List<QueueItem>, startIndex: Int) {
        require(startIndex in queue.indices) { "startIndex $startIndex is not in the queue of ${queue.size}" }
        val request = Play(queue, startIndex)
        val controller = controller
        if (controller == null) {
            pendingPlay = request
            _positionMs.value = 0L
            // Попытки исчерпаны: тап пользователя — повод начать новую серию, иначе play потерялся бы.
            if (_state.value == PlaybackState.ConnectionFailed) connect()
            return
        }
        start(controller, request)
    }

    override fun togglePlayPause() {
        controller?.let(Util::handlePlayPauseButtonAction)
    }

    override fun seekTo(positionMs: Long) {
        val controller = controller ?: return
        controller.seekTo(positionMs)
        _positionMs.value = positionMs
    }

    override fun next() {
        controller?.seekToNext()
    }

    override fun previous() {
        controller?.seekToPrevious()
    }

    override fun seekBack() {
        controller?.seekBack()
    }

    override fun seekForward() {
        controller?.seekForward()
    }

    private fun connect() {
        if (controllerFuture != null || retryJob?.isActive == true) return
        // Новая серия попыток после неудачи начинается с «подключаемся», не с ошибки.
        if (_state.value == PlaybackState.ConnectionFailed) _state.value = PlaybackState.Idle
        attempts++
        val future = connector.connect(controllerListener)
        controllerFuture = future
        // The future completes on the main thread, where the controller lives.
        future.addListener({ onConnected(future) }, MoreExecutors.directExecutor())
    }

    private fun onConnected(future: ListenableFuture<MediaController>) {
        if (future !== controllerFuture) return
        val connected = try {
            Futures.getDone(future)
        } catch (e: ExecutionException) {
            Log.w(TAG, "Could not connect to the playback session (attempt $attempts)", e.cause)
            controllerFuture = null
            retryOrFail()
            return
        } catch (e: CancellationException) {
            controllerFuture = null
            return
        }
        attempts = 0
        controller = connected
        sessionReady = !connected.sessionExtras.isRestoring()
        connected.addListener(playerListener)
        val request = pendingPlay
        pendingPlay = null
        if (request != null) {
            // Publishes the snapshot with the new queue: a deferred play never shows the empty
            // queue of a fresh session, which would close the player screen.
            start(connected, request)
        } else {
            queue = connected.readQueue()
            publish(connected)
        }
        // Позиция пустой, ещё восстанавливающейся сессии не показывается — её даст onExtrasChanged.
        if (sessionReady) {
            _positionMs.value = connected.currentPosition
            updatePositionPolling(connected)
        }
    }

    private fun retryOrFail() {
        if (attempts >= MAX_CONNECT_ATTEMPTS) {
            attempts = 0
            _state.value = PlaybackState.ConnectionFailed
            return
        }
        val pause = CONNECT_RETRY_BASE_MS shl (attempts - 1)
        retryJob = scope.launch(mainDispatcher) {
            delay(pause)
            retryJob = null
            connect()
        }
    }

    private fun disconnect() {
        stopPositionPolling()
        retryJob?.cancel()
        retryJob = null
        attempts = 0
        controller?.removeListener(playerListener)
        controller = null
        controllerFuture?.let(MediaController::releaseFuture)
        controllerFuture = null
        sessionReady = false
        // A tap that never got connected is dropped: it must not start playing hours later.
        pendingPlay = null
        queue = emptyList()
        // Последний снапшот остаётся видимым, но помечен: команды до сессии не дойдут.
        _state.update { state -> (state as? PlaybackState.Active)?.copy(isConnected = false) ?: state }
    }

    private fun start(controller: MediaController, request: Play) {
        // Своя очередь показывается сразу: восстановление сессии её уже не перетрёт.
        sessionReady = true
        // Сравнение — со снапшотом самой сессии, не с _state: у отложенного play _state ещё Idle,
        // и тап по играющему треку сразу после возврата из фона перезапускал бы его с нуля.
        queue = controller.readQueue()
        val current = controller.toPlaybackState(queue)
        if (current.queue == request.queue && current.currentIndex == request.startIndex) {
            Util.handlePlayButtonAction(controller)
            publish(controller)
            return
        }
        controller.setMediaItems(request.queue.map { it.toMediaItem() }, request.startIndex, 0L)
        controller.prepare()
        controller.play()
        _positionMs.value = 0L
        // The controller masks the commands at once; publish now, not on the next player event,
        // so a screen opened right after this call and a repeated tap see the new queue.
        queue = controller.readQueue()
        publish(controller)
    }

    private fun publish(player: Player) {
        if (!sessionReady) return
        _state.value = player.toPlaybackState(queue)
    }

    private fun Bundle.isRestoring(): Boolean = getBoolean(EXTRA_SESSION_RESTORING, false)

    private fun updatePositionPolling(player: Player) {
        if (!player.isPlaying) {
            stopPositionPolling()
            _positionMs.value = player.currentPosition
            return
        }
        if (positionJob?.isActive == true) return
        positionJob = scope.launch(mainDispatcher) {
            while (isActive) {
                _positionMs.value = player.currentPosition
                delay(POSITION_POLL_MS)
            }
        }
    }

    private fun stopPositionPolling() {
        positionJob?.cancel()
        positionJob = null
    }

    private data class Play(val queue: List<QueueItem>, val startIndex: Int)

    private companion object {
        const val TAG = "PlaybackController"
        const val POSITION_POLL_MS = 500L
    }
}
