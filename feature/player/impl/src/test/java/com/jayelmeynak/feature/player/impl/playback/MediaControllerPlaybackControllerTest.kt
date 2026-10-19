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
import com.jayelmeynak.feature.player.impl.service.testSessionCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.jayelmeynak.feature.player.impl.service.finishRestoring
import com.jayelmeynak.feature.player.impl.service.sessionExtras
import com.jayelmeynak.feature.player.impl.service.toMediaItem
import com.jayelmeynak.feature.player.impl.service.withTrackUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
 *
 * Общий контракт и доставка транспортных команд до плеера сессии — в
 * [MediaControllerPlaybackControllerContractTest] (часы плеера там стоят); здесь — поведение
 * реализации, которому нужна игра по часам: переходы, конец очереди, опрос позиции, восстановление.
 */
@RunWith(RobolectricTestRunner::class)
class MediaControllerPlaybackControllerTest {

    private val context = RuntimeEnvironment.getApplication()
    private val dispatcher = StandardTestDispatcher()
    private val scope = CoroutineScope(SupervisorJob())
    private val lifecycle = TestProcessLifecycle()
    private lateinit var player: ExoPlayer
    private lateinit var session: MediaSession
    private val futures = mutableListOf<ListenableFuture<MediaController>>()

    /** Контроллер теста; создаётся при первом обращении, тесты со своим коннектором его не создают. */
    private val controller: MediaControllerPlaybackController by lazy {
        MediaControllerPlaybackController(
            lifecycleOwner = lifecycle,
            connector = ::realConnect,
            scope = scope,
            mainDispatcher = dispatcher,
        )
    }

    @Before
    fun setUp() {
        player = TestExoPlayerBuilder(context).setMediaSourceFactory(FakeMediaSourceFactory()).build()
        session = MediaSession.Builder(context, player).setCallback(testSessionCallback()).build()
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
    fun `создание контроллера вне главного потока падает с понятной ошибкой`() {
        var error: Throwable? = null
        val thread = Thread {
            error = runCatching {
                MediaControllerPlaybackController(
                    lifecycleOwner = lifecycle,
                    connector = { error("подключение не ожидается") },
                    scope = scope,
                    mainDispatcher = dispatcher,
                )
            }.exceptionOrNull()
        }
        thread.start()
        thread.join()

        assertTrue(error is IllegalStateException)
        assertTrue(error?.message.orEmpty().contains("главном потоке"))
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
    fun `сессия оборвалась - снапшот не подключён и переподключение к новой сессии`() {
        lifecycle.start()
        connectedPlay(queue, 0)
        val states = recordStates()
        // The next connection finds a new, empty session, as a recreated service would give.
        val oldSession = session
        val newPlayer = TestExoPlayerBuilder(context).setMediaSourceFactory(FakeMediaSourceFactory()).build()
        extraPlayers += newPlayer
        session = MediaSession.Builder(context, newPlayer)
            .setId("recreated")
            .setCallback(testSessionCallback())
            .build()

        oldSession.release()

        runMainLooperUntil { futures.size == 2 && (controller.state.value as? PlaybackState.Active)?.isConnected == true }
        assertFalse(states.contains(PlaybackState.Idle))
        assertTrue(states.any { it is PlaybackState.Active && !it.isConnected && it.queue == queue })
        assertTrue((controller.state.value as PlaybackState.Active).queue.isEmpty())
    }

    @Test
    fun `обрыв сессии в фоне - без переподключения`() {
        lifecycle.start()
        connectedPlay(queue, 0)
        lifecycle.stop()

        session.release()
        drainMainLooper()

        assertEquals(false, (controller.state.value as PlaybackState.Active).isConnected)
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
        drainMainLooper()

        assertEquals(0, player.mediaItemCount)
        assertTrue((slow.state.value as PlaybackState.Active).queue.isEmpty())
    }

    @Test
    fun `транспортные команды до подключения не запускают подключение`() {
        controller.togglePlayPause()
        controller.seekTo(1_000L)
        controller.next()
        controller.previous()
        controller.seekBack()
        controller.seekForward()

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
    fun `ON_STOP - последний снапшот не подключён и контроллер освобождён, ON_START - снапшот текущего плеера`() {
        lifecycle.start()
        connectedPlay(queue, 2)

        lifecycle.stop()
        val stopped = controller.state.value as PlaybackState.Active
        assertEquals(queue, stopped.queue)
        assertEquals(2, stopped.currentIndex)
        assertFalse(stopped.isConnected)

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
    fun `повторный play того же трека той же очереди не перезапускает его`() {
        lifecycle.start()
        connectedPlay(queue, 1)
        var playlistChanges = 0
        player.addListener(object : Player.Listener {
            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
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
        // Трек уже не в начале: перемотка прямо в плеере сессии — контроллер в этот момент уходит в фон.
        player.seekTo(1, SEEK_POSITION_MS)
        val playlistChanges = countPlaylistChanges()
        lifecycle.stop()
        val before = player.currentPosition
        assertTrue(before > 0)

        controller.play(queue, 1)
        lifecycle.start()
        runMainLooperUntil { (controller.state.value as? PlaybackState.Active)?.currentIndex == 1 }
        drainMainLooper()

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

        // MediaController экстраполирует позицию по SystemClock, а Robolectric двигает его только по
        // запросу.
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(4))
        dispatcher.scheduler.advanceTimeBy(POLL_STEP_MS)
        assertTrue(controller.positionMs.value > before)

        controller.togglePlayPause()
        runMainLooperUntil { (controller.state.value as? PlaybackState.Active)?.isPlaying == false }
        // Проверка — само завершение: бесконечный цикл опроса не дал бы планировщику дойти до простоя.
        // Сравнивать позицию до и после бессмысленно — на паузе MediaController отдаёт одну и ту же.
        dispatcher.scheduler.advanceUntilIdle()
        assertFalse((controller.state.value as PlaybackState.Active).isPlaying)
    }

    @Test
    fun `возврат из фона - ни одного пустого или Idle снапшота между старым и новым`() {
        lifecycle.start()
        connectedPlay(queue, 1)
        lifecycle.stop()
        val states = recordStates()

        lifecycle.start()
        runMainLooperUntil { (controller.state.value as? PlaybackState.Active)?.isConnected == true }

        assertTrue(states.none { it == PlaybackState.Idle })
        assertTrue(states.filterIsInstance<PlaybackState.Active>().all { it.queue == queue })
    }

    @Test
    fun `не подключён - транспортные команды не доходят до сессии`() {
        lifecycle.start()
        connectedPlay(queue, 0)
        lifecycle.stop()

        controller.next()
        controller.togglePlayPause()
        drainMainLooper()

        assertEquals(0, player.currentMediaItemIndex)
        assertTrue(player.playWhenReady)
    }

    @Test
    fun `сессия восстанавливает очередь - снапшот публикуется только после восстановления`() {
        val restoringSession = restoringSession()
        val states = recordStates()

        lifecycle.start()
        runMainLooperUntil { futures.isNotEmpty() && futures.last().isDone }
        drainMainLooper()
        assertEquals(PlaybackState.Idle, controller.state.value)

        player.setMediaItems(queue.map { it.toMediaItem().withTrackUri() }, 2, 0L)
        collector.launch(Dispatchers.Main.immediate) { restoringSession.finishRestoring() }

        runMainLooperUntil { controller.state.value is PlaybackState.Active }
        val active = states.filterIsInstance<PlaybackState.Active>()
        assertTrue(active.none { it.queue.isEmpty() })
        assertEquals(queue, active.first().queue)
        assertEquals(2, active.first().currentIndex)
    }

    @Test
    fun `возврат к восстанавливающейся сессии - позиция прежнего снапшота не сбрасывается в 0`() {
        lifecycle.start()
        connectedPlay(queue, 0)
        player.seekTo(0, SEEK_POSITION_MS)
        runMainLooperUntil { controller.positionMs.value >= SEEK_POSITION_MS }
        lifecycle.stop()
        val before = controller.positionMs.value
        restoringSession(emptyPlayer = true)

        lifecycle.start()
        runMainLooperUntil { futures.size == 2 && futures.last().isDone }
        drainMainLooper()

        assertEquals(before, controller.positionMs.value)
        assertFalse((controller.state.value as PlaybackState.Active).isConnected)
    }

    @Test
    fun `отложенный play во время восстановления сессии публикуется сразу`() {
        restoringSession()
        controller.play(queue, 1)

        lifecycle.start()

        runMainLooperUntil { (controller.state.value as? PlaybackState.Active)?.currentIndex == 1 }
        assertEquals(queue, (controller.state.value as PlaybackState.Active).queue)
    }

    @Test
    fun `подключение не удалось один раз - повтор через паузу подключается`() {
        var failures = 1
        val retrying = controllerWith { listener ->
            if (failures-- > 0) Futures.immediateFailedFuture(SecurityException("сбой в тесте")) else realConnect(listener)
        }

        lifecycle.start()
        assertEquals(PlaybackState.Idle, retrying.state.value)
        dispatcher.scheduler.advanceTimeBy(CONNECT_RETRY_BASE_MS - 1)
        dispatcher.scheduler.runCurrent()
        assertTrue(futures.isEmpty())

        dispatcher.scheduler.advanceTimeBy(2)
        dispatcher.scheduler.runCurrent()
        runMainLooperUntil { retrying.state.value is PlaybackState.Active }
    }

    @Test
    fun `все попытки подключения неудачны - ConnectionFailed после лимита, паузы растут`() {
        val attemptsAt = mutableListOf<Long>()
        val failing = controllerWith {
            attemptsAt += dispatcher.scheduler.currentTime
            Futures.immediateFailedFuture(SecurityException("сбой в тесте"))
        }

        lifecycle.start()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(0L, 500L, 1_500L, 3_500L), attemptsAt)
        assertEquals(PlaybackState.ConnectionFailed, failing.state.value)
    }

    @Test
    fun `уход в фон отменяет повтор, возврат начинает попытки заново`() {
        var attempts = 0
        val failing = controllerWith {
            attempts++
            Futures.immediateFailedFuture(SecurityException("сбой в тесте"))
        }
        lifecycle.start()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(PlaybackState.ConnectionFailed, failing.state.value)

        lifecycle.stop()
        lifecycle.start()
        assertEquals(PlaybackState.Idle, failing.state.value)
        dispatcher.scheduler.runCurrent()
        lifecycle.stop()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(MAX_CONNECT_ATTEMPTS + 1, attempts)
    }

    @Test
    fun `play после ConnectionFailed начинает новую серию подключения и играет`() {
        var failures = MAX_CONNECT_ATTEMPTS
        val retrying = controllerWith { listener ->
            if (failures-- > 0) Futures.immediateFailedFuture(SecurityException("сбой в тесте")) else realConnect(listener)
        }
        lifecycle.start()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(PlaybackState.ConnectionFailed, retrying.state.value)

        retrying.play(queue, 1)

        runMainLooperUntil { (retrying.state.value as? PlaybackState.Active)?.currentIndex == 1 }
        assertEquals(queue, (retrying.state.value as PlaybackState.Active).queue)
        runMainLooperUntil { player.playWhenReady && player.currentMediaItemIndex == 1 }
    }

    @Test
    fun `сессия ещё восстанавливается - транспортная команда доходит до сессии`() {
        player.setMediaItems(queue.map { it.toMediaItem().withTrackUri() }, 0, 0L)
        restoringSession()
        val states = recordStates()
        lifecycle.start()
        runMainLooperUntil { futures.isNotEmpty() && futures.last().isDone }
        drainMainLooper()
        assertEquals(listOf<PlaybackState>(PlaybackState.Idle), states)

        controller.next()

        runMainLooperUntil { player.currentMediaItemIndex == 1 }
    }

    /**
     * Заменяет сессию теста новой, которая сообщает «восстановление идёт»; [emptyPlayer] — над новым
     * пустым плеером, как у пересозданного сервиса.
     */
    private fun restoringSession(emptyPlayer: Boolean = false): MediaSession {
        session.release()
        val sessionPlayer = if (emptyPlayer) {
            TestExoPlayerBuilder(context).setMediaSourceFactory(FakeMediaSourceFactory()).build().also { extraPlayers += it }
        } else {
            player
        }
        session = MediaSession.Builder(context, sessionPlayer)
            .setId("restoring")
            .setCallback(testSessionCallback())
            .setSessionExtras(sessionExtras(restoring = true))
            .build()
        return session
    }

    private fun realConnect(listener: MediaController.Listener): ListenableFuture<MediaController> =
        MediaController.Builder(context, session.token).setListener(listener).buildAsync().also { futures += it }

    private fun controllerWith(
        connector: (MediaController.Listener) -> ListenableFuture<MediaController>,
    ): MediaControllerPlaybackController {
        return MediaControllerPlaybackController(
            lifecycleOwner = lifecycle,
            connector = connector,
            scope = scope,
            mainDispatcher = dispatcher,
        )
    }

    /**
     * Выполняет все задачи главного лупера, в том числе поставленные по ходу, — перед проверкой «ничего
     * не произошло». In-process контроллер и сессия общаются только через главный лупер, поэтому после
     * него команда контроллера уже дошла бы до плеера, а ответ сессии — до контроллера.
     */
    private fun drainMainLooper() {
        shadowOf(Looper.getMainLooper()).idle()
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
