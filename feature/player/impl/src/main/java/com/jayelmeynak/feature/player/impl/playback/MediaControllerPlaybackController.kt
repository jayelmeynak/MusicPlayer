package com.jayelmeynak.feature.player.impl.playback

import android.util.Log
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import javax.inject.Inject
import javax.inject.Singleton

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
 * Public only for the binding in `:di`; nothing outside the module can create it.
 */
@Singleton
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
    }

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (events.contains(Player.EVENT_TIMELINE_CHANGED)) queue = player.readQueue()
            publish(player)
            if (events.containsAny(
                    Player.EVENT_POSITION_DISCONTINUITY,
                    Player.EVENT_MEDIA_ITEM_TRANSITION,
                    Player.EVENT_TIMELINE_CHANGED,
                )
            ) {
                _positionMs.value = player.currentPosition
            }
            if (events.contains(Player.EVENT_IS_PLAYING_CHANGED)) updatePositionPolling(player)
        }
    }

    init {
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
        if (controllerFuture != null) return
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
            // Stay idle; the next process start tries again.
            Log.w(TAG, "Could not connect to the playback session", e.cause)
            controllerFuture = null
            return
        } catch (e: CancellationException) {
            controllerFuture = null
            return
        }
        controller = connected
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
        _positionMs.value = connected.currentPosition
        updatePositionPolling(connected)
    }

    private fun disconnect() {
        stopPositionPolling()
        controller?.removeListener(playerListener)
        controller = null
        controllerFuture?.let(MediaController::releaseFuture)
        controllerFuture = null
        // A tap that never got connected is dropped: it must not start playing hours later.
        pendingPlay = null
        queue = emptyList()
        _state.value = PlaybackState.Idle
    }

    private fun start(controller: MediaController, request: Play) {
        val current = _state.value as? PlaybackState.Active
        if (current != null && current.queue == request.queue && current.currentIndex == request.startIndex) {
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
        _state.value = player.toPlaybackState(queue)
    }

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
