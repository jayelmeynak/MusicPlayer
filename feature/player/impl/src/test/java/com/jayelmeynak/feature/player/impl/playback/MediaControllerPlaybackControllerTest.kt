package com.jayelmeynak.feature.player.impl.playback

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.MediaSession
import androidx.media3.test.utils.FakeMediaSourceFactory
import androidx.media3.test.utils.TestExoPlayerBuilder
import androidx.media3.test.utils.robolectric.RobolectricUtil.runMainLooperUntil
import com.jayelmeynak.feature.player.api.PlaybackState
import com.jayelmeynak.feature.player.api.QueueItem
import com.jayelmeynak.feature.player.api.TrackSource
import com.jayelmeynak.feature.player.impl.service.PlaybackSessionCallback
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.robolectric.shadows.ShadowLooper
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import android.os.Looper
import org.robolectric.Shadows.shadowOf
import java.time.Duration

/**
 * Robolectric runs an in-process media session over a test ExoPlayer; the controller connects to
 * its token instead of the service. Media3 delivers everything through the main looper, which the
 * test runs explicitly (`runMainLooperUntil`); position polling runs on a test dispatcher.
 */
@RunWith(RobolectricTestRunner::class)
class MediaControllerPlaybackControllerTest {

    private val context = RuntimeEnvironment.getApplication()
    private val dispatcher = StandardTestDispatcher()
    private val scope = CoroutineScope(SupervisorJob())
    private val lifecycle = TestProcessLifecycle()
    private lateinit var player: ExoPlayer
    private lateinit var session: MediaSession
    private val futures = mutableListOf<com.google.common.util.concurrent.ListenableFuture<MediaController>>()
    private lateinit var controller: MediaControllerPlaybackController

    @Before
    fun setUp() {
        player = TestExoPlayerBuilder(context).setMediaSourceFactory(FakeMediaSourceFactory()).build()
        session = MediaSession.Builder(context, player).setCallback(PlaybackSessionCallback()).build()
        controller = MediaControllerPlaybackController(
            lifecycleOwner = lifecycle,
            connector = { listener ->
                MediaController.Builder(context, session.token)
                    .setListener(listener)
                    .buildAsync()
                    .also { futures += it }
            },
            scope = scope,
            mainDispatcher = dispatcher,
        )
    }

    @After
    fun tearDown() {
        lifecycle.registry.currentState = Lifecycle.State.DESTROYED
        futures.forEach { MediaController.releaseFuture(it) }
        session.release()
        player.release()
        extraPlayers.forEach { it.release() }
        scope.cancel()
        collector.cancel()
    }

    private val extraPlayers = mutableListOf<ExoPlayer>()
    private val collector = CoroutineScope(Dispatchers.Unconfined)

    /** Every value [MediaControllerPlaybackController.state] takes, in order. */
    private fun recordStates(): List<PlaybackState> {
        val states = mutableListOf<PlaybackState>()
        collector.launch { controller.state.collect { states += it } }
        return states
    }

    @Test
    fun `отложенный play - после подключения ни одного снапшота с пустой очередью`() {
        val states = recordStates()
        controller.play(queue, 1)

        lifecycle.start()
        runMainLooperUntil { (controller.state.value as? PlaybackState.Active)?.currentIndex == 1 }
        runMainLooperUntil { player.playWhenReady }

        val active = states.filterIsInstance<PlaybackState.Active>()
        assertTrue(active.isNotEmpty())
        assertTrue(active.none { it.queue.isEmpty() })
        assertEquals(queue, active.first().queue)
    }

    @Test
    fun `play при подключённом контроллере сразу публикует новую очередь`() {
        lifecycle.start()
        connectedPlay(queue, 0)
        val next = queue.reversed()

        controller.play(next, 2)

        val state = controller.state.value as PlaybackState.Active
        assertEquals(next, state.queue)
        assertEquals(2, state.currentIndex)
    }

    @Test
    fun `сессия оборвалась - Idle и переподключение к новой сессии`() {
        lifecycle.start()
        connectedPlay(queue, 0)
        val states = recordStates()
        // The next connection finds a new, empty session, as a recreated service would give.
        val oldSession = session
        val newPlayer = TestExoPlayerBuilder(context).setMediaSourceFactory(FakeMediaSourceFactory()).build()
        extraPlayers += newPlayer
        session = MediaSession.Builder(context, newPlayer)
            .setId("recreated")
            .setCallback(PlaybackSessionCallback())
            .build()

        oldSession.release()

        runMainLooperUntil { futures.size == 2 && controller.state.value is PlaybackState.Active }
        assertTrue(states.contains(PlaybackState.Idle))
        assertTrue((controller.state.value as PlaybackState.Active).queue.isEmpty())
    }

    @Test
    fun `обрыв сессии в фоне - без переподключения`() {
        lifecycle.start()
        connectedPlay(queue, 0)
        lifecycle.stop()

        session.release()
        repeat(5) { ShadowLooper.idleMainLooper() }

        assertEquals(PlaybackState.Idle, controller.state.value)
        assertEquals(1, futures.size)
    }

    @Test
    fun `play до подключения забывается при уходе в фон`() {
        val connections = mutableListOf<SettableFuture<MediaController>>()
        val slow = MediaControllerPlaybackController(
            lifecycleOwner = lifecycle,
            // The first connection never completes: the app goes to background before it does.
            connector = { listener ->
                if (connections.isEmpty()) {
                    SettableFuture.create<MediaController>().also { connections += it }
                } else {
                    MediaController.Builder(context, session.token)
                        .setListener(listener)
                        .buildAsync()
                        .also { futures += it }
                }
            },
            scope = scope,
            mainDispatcher = dispatcher,
        )
        slow.play(queue, 0)
        lifecycle.start()
        lifecycle.stop()

        lifecycle.start()
        runMainLooperUntil { slow.state.value is PlaybackState.Active }
        repeat(5) { ShadowLooper.idleMainLooper() }

        assertEquals(0, player.mediaItemCount)
        assertTrue((slow.state.value as PlaybackState.Active).queue.isEmpty())
    }

    @Test
    fun `до подключения - Idle, транспортные команды игнорируются`() {
        controller.togglePlayPause()
        controller.next()
        controller.seekTo(1_000L)

        assertEquals(PlaybackState.Idle, controller.state.value)
        assertTrue(futures.isEmpty())
    }

    @Test
    fun `play до подключения исполняется после него, выигрывает последний`() {
        controller.play(queue, 0)
        controller.play(queue, 1)
        assertEquals(PlaybackState.Idle, controller.state.value)

        lifecycle.start()
        runMainLooperUntil { player.playWhenReady && player.mediaItemCount == queue.size }
        runMainLooperUntil { (controller.state.value as? PlaybackState.Active)?.queue == queue }

        val state = controller.state.value as PlaybackState.Active
        assertEquals(1, state.currentIndex)
        assertEquals(1, player.currentMediaItemIndex)
        assertTrue(state.playWhenReady)
    }

    @Test
    fun `после подключения к пустой сессии - Active с пустой очередью`() {
        lifecycle.start()

        runMainLooperUntil { controller.state.value is PlaybackState.Active }

        assertTrue((controller.state.value as PlaybackState.Active).queue.isEmpty())
    }

    @Test
    fun `ON_STOP - Idle и контроллер освобождён, ON_START - снапшот текущего плеера`() {
        lifecycle.start()
        connectedPlay(queue, 2)

        lifecycle.stop()
        assertEquals(PlaybackState.Idle, controller.state.value)

        lifecycle.start()
        runMainLooperUntil { controller.state.value is PlaybackState.Active }
        val state = controller.state.value as PlaybackState.Active
        assertEquals(queue, state.queue)
        assertEquals(2, state.currentIndex)
        assertEquals(2, futures.size)
    }

    @Test
    fun `next извне сессии (уведомление) - новый индекс в состоянии`() {
        lifecycle.start()
        connectedPlay(queue, 0)

        player.seekToNext()

        runMainLooperUntil { (controller.state.value as? PlaybackState.Active)?.currentIndex == 1 }
        assertEquals(queue[1], (controller.state.value as PlaybackState.Active).current)
    }

    @Test
    fun `next и пауза из UI доходят до плеера сессии`() {
        lifecycle.start()
        connectedPlay(queue, 0)

        controller.next()
        runMainLooperUntil { player.currentMediaItemIndex == 1 }
        controller.togglePlayPause()
        runMainLooperUntil { !player.playWhenReady }

        runMainLooperUntil { (controller.state.value as? PlaybackState.Active)?.playWhenReady == false }
    }

    @Test
    fun `повторный play того же трека той же очереди не перезапускает его`() {
        lifecycle.start()
        connectedPlay(queue, 1)
        var playlistChanges = 0
        player.addListener(object : Player.Listener {
            override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
                if (reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) playlistChanges++
            }
        })

        controller.play(queue, 1)
        runMainLooperUntil { player.isPlaying }

        assertEquals(0, playlistChanges)
        assertEquals(1, player.currentMediaItemIndex)
    }

    @Test
    fun `отложенный play той же очереди и трека при возврате из фона не перезапускает трек`() {
        lifecycle.start()
        connectedPlay(queue, 1)
        // Трек уже не в начале: перемотка прямо в плеере сессии (seek контроллера под Robolectric
        // до сессии не доходит).
        player.seekTo(1, SEEK_POSITION_MS)
        val playlistChanges = countPlaylistChanges()
        lifecycle.stop()
        val before = player.currentPosition
        assertTrue(before > 0)

        controller.play(queue, 1)
        lifecycle.start()
        runMainLooperUntil { (controller.state.value as? PlaybackState.Active)?.currentIndex == 1 }
        repeat(5) { ShadowLooper.idleMainLooper() }

        assertEquals(0, playlistChanges())
        assertEquals(1, player.currentMediaItemIndex)
        assertTrue(player.currentPosition >= before)
        assertTrue(player.playWhenReady)
        assertEquals(queue, (controller.state.value as PlaybackState.Active).queue)
    }

    @Test
    fun `повторный play того же трека после остановки плеера - играет снова`() {
        lifecycle.start()
        connectedPlay(queue, 1)
        player.stop()
        runMainLooperUntil { (controller.state.value as? PlaybackState.Active)?.isPlaying == false }
        val playlistChanges = countPlaylistChanges()

        controller.play(queue, 1)
        runMainLooperUntil { player.isPlaying }

        assertEquals(0, playlistChanges())
        assertEquals(1, player.currentMediaItemIndex)
    }

    @Test
    fun `повторный play того же трека на паузе - продолжает с места паузы`() {
        lifecycle.start()
        connectedPlay(queue, 0)
        player.seekTo(0, SEEK_POSITION_MS)
        controller.togglePlayPause()
        runMainLooperUntil { !player.playWhenReady }
        val paused = player.currentPosition
        assertTrue(paused > 0)
        val playlistChanges = countPlaylistChanges()

        controller.play(queue, 0)
        runMainLooperUntil { player.isPlaying }

        assertEquals(0, playlistChanges())
        assertEquals(0, player.currentMediaItemIndex)
        assertTrue(player.currentPosition >= paused)
    }

    @Test
    fun `повторный play того же трека в конце очереди - с начала трека, очередь не заменяется`() {
        lifecycle.start()
        connectedPlay(queue, 2)
        runMainLooperUntil { player.playbackState == Player.STATE_ENDED }
        val playlistChanges = countPlaylistChanges()

        controller.play(queue, 2)
        runMainLooperUntil { player.playbackState != Player.STATE_ENDED && player.playWhenReady }

        assertEquals(0, playlistChanges())
        assertEquals(2, player.currentMediaItemIndex)
        assertTrue(player.currentPosition < SEEK_POSITION_MS)
        assertEquals(queue, (controller.state.value as PlaybackState.Active).queue)
    }

    @Test
    fun `позиция опрашивается во время игры и не опрашивается на паузе`() {
        lifecycle.start()
        connectedPlay(queue, 0)
        runMainLooperUntil { (controller.state.value as? PlaybackState.Active)?.isPlaying == true }
        dispatcher.scheduler.advanceTimeBy(POLL_STEP_MS + 1)
        val before = controller.positionMs.value

        // The session reports the position periodically; the controller extrapolates it with the
        // system clock, which Robolectric moves only on request.
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(4))
        dispatcher.scheduler.advanceTimeBy(POLL_STEP_MS)
        assertTrue(controller.positionMs.value > before)

        controller.togglePlayPause()
        runMainLooperUntil { (controller.state.value as? PlaybackState.Active)?.isPlaying == false }
        val paused = controller.positionMs.value
        // An endless polling loop would never let the scheduler become idle.
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(paused, controller.positionMs.value)
        assertFalse((controller.state.value as PlaybackState.Active).isPlaying)
    }

    @Test
    fun `seek на паузе сразу обновляет позицию`() {
        lifecycle.start()
        connectedPlay(queue, 0)
        controller.togglePlayPause()
        runMainLooperUntil { !player.playWhenReady }

        controller.seekTo(5_000L)

        assertEquals(5_000L, controller.positionMs.value)
        // Under Robolectric the seek command of MediaController does not reach the session player
        // (next and pause do); delivery is checked manually on a device.
    }

    @Test
    fun `play сбрасывает позицию в 0`() {
        lifecycle.start()
        connectedPlay(queue, 0)
        controller.seekTo(5_000L)

        controller.play(queue.reversed(), 0)

        assertEquals(0L, controller.positionMs.value)
    }

    /** Считает замены плейлиста плеера сессии с момента вызова. */
    private fun countPlaylistChanges(): () -> Int {
        var changes = 0
        player.addListener(object : Player.Listener {
            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                if (reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) changes++
            }
        })
        return { changes }
    }

    private fun connectedPlay(queue: List<QueueItem>, index: Int) {
        runMainLooperUntil { controller.state.value is PlaybackState.Active }
        controller.play(queue, index)
        runMainLooperUntil { player.playWhenReady && player.currentMediaItemIndex == index }
        runMainLooperUntil { (controller.state.value as? PlaybackState.Active)?.currentIndex == index }
    }

    /** Stands in for the process lifecycle: the test moves it between CREATED and STARTED. */
    private class TestProcessLifecycle : LifecycleOwner {
        val registry: LifecycleRegistry = LifecycleRegistry.createUnsafe(this).apply {
            currentState = Lifecycle.State.CREATED
        }
        override val lifecycle: Lifecycle get() = registry

        fun start() {
            registry.currentState = Lifecycle.State.STARTED
        }

        fun stop() {
            registry.currentState = Lifecycle.State.CREATED
        }
    }

    private val queue = listOf(
        QueueItem("1", TrackSource.DEEZER, "One", "A", null, 200_000L),
        QueueItem("2", TrackSource.DEEZER, "Two", "B", null, 0L),
        QueueItem("content://media/external/audio/media/3", TrackSource.LOCAL, "Three", "C", null, 3_000L),
    )

    private companion object {
        const val POLL_STEP_MS = 500L
        const val SEEK_POSITION_MS = 3_000L
    }
}
