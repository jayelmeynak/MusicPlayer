package com.jayelmeynak.feature.player.impl.playback

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.MediaSession
import androidx.media3.test.utils.FakeClock
import androidx.media3.test.utils.FakeMediaSourceFactory
import androidx.media3.test.utils.TestExoPlayerBuilder
import androidx.media3.test.utils.robolectric.RobolectricUtil.runMainLooperUntil
import com.google.common.util.concurrent.ListenableFuture
import com.jayelmeynak.feature.player.api.PlaybackController
import com.jayelmeynak.feature.player.api.PlaybackState
import com.jayelmeynak.feature.player.api.testing.PlaybackControllerContract
import com.jayelmeynak.feature.player.impl.service.testSessionCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowLooper
import android.os.Looper
import java.time.Duration
import java.util.concurrent.TimeoutException

/**
 * Общий контракт контроллера на настоящей реализации и доставка транспортных команд до плеера сессии.
 *
 * Robolectric — ради in-process `MediaSession` над `TestExoPlayer`: контроллер подключается к её
 * токену, Media3 доставляет всё через главный лупер. Часы плеера стоят (`FakeClock` без
 * автопродвижения): позиция и текущий элемент не уходят вперёд, пока лупер крутится, поэтому
 * проверки позиции точные. Опрос позиции — на тестовом диспетчере, который тест не двигает.
 */
@RunWith(RobolectricTestRunner::class)
class MediaControllerPlaybackControllerContractTest : PlaybackControllerContract() {

    private val context = RuntimeEnvironment.getApplication()
    private val scope = CoroutineScope(SupervisorJob())
    private val lifecycle = TestProcessLifecycle()
    private val futures = mutableListOf<ListenableFuture<MediaController>>()

    private val clock = FakeClock(/* isAutoAdvancing= */ false)

    private val player: ExoPlayer = TestExoPlayerBuilder(context)
        .setMediaSourceFactory(FakeMediaSourceFactory())
        .setClock(clock)
        .setSeekBackIncrementMs(SEEK_BACK_MS)
        .setSeekForwardIncrementMs(SEEK_FORWARD_MS)
        .setMaxSeekToPreviousPositionMs(MAX_SEEK_TO_PREVIOUS_MS)
        .build()

    private val session: MediaSession = MediaSession.Builder(context, player)
        .setCallback(testSessionCallback())
        .build()

    override val controller: PlaybackController = MediaControllerPlaybackController(
        lifecycleOwner = lifecycle,
        connector = { listener ->
            MediaController.Builder(context, session.token).setListener(listener).buildAsync().also { futures += it }
        },
        scope = scope,
        mainDispatcher = StandardTestDispatcher(),
    )

    override fun connect() {
        lifecycle.start()
        awaitMainLooper("контроллер не получил подключённый снапшот") {
            (controller.state.value as? PlaybackState.Active)?.isConnected == true
        }
    }

    override fun disconnect() = lifecycle.stop()

    /**
     * Ждёт, пока текущий элемент станет перематываемым и это увидит контроллер. До подготовки
     * источника у элемента временный таймлайн без перемотки, и `MediaController` молча отбрасывает
     * `seekTo` (`COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM` недоступна): без этого ожидания тест перемотки
     * проверял бы гонку, а не доставку. Подготовка идёт по часам плеера — пока трек не готов, часы
     * двигаются по миллисекунде; готовый трек на стоящих часах не играет дальше, позиция точная.
     */
    override fun awaitSeekable() {
        awaitMainLooper("плеер сессии не подготовил трек") {
            (player.playbackState == Player.STATE_READY).also { ready -> if (!ready) clock.advanceTime(1) }
        }
        awaitMainLooper("контроллер не увидел длительность подготовленного трека") {
            (controller.state.value as? PlaybackState.Active)?.durationMs == FAKE_WINDOW_DURATION_MS
        }
        // Доступные команды сессия рассылает отдельным сообщением главного лупера: дочитываем очередь.
        ShadowLooper.idleMainLooper()
    }

    @After
    fun tearDown() {
        lifecycle.registry.currentState = Lifecycle.State.DESTROYED
        futures.forEach { MediaController.releaseFuture(it) }
        session.release()
        player.release()
        scope.cancel()
    }

    @Test
    fun `seekTo доходит до плеера сессии`() {
        playReady(0)

        controller.seekTo(SEEK_MS)

        awaitPlayerPosition(0, SEEK_MS, "перемотка контроллера не дошла до плеера сессии")
    }

    @Test
    fun `seekTo на паузе доходит до плеера сессии`() {
        playReady(0)
        controller.togglePlayPause()
        awaitMainLooper("пауза не дошла до состояния контроллера") {
            !player.playWhenReady && (controller.state.value as? PlaybackState.Active)?.playWhenReady == false
        }
        // На паузе MediaController верит только снапшотам позиции новее своей команды паузы, а
        // SystemClock Robolectric стоит: без сдвига и маска перемотки, и ответ сессии остались бы
        // со старой позицией, и контроллер вернул бы её в positionMs.
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(10))

        controller.seekTo(SEEK_MS)

        awaitPlayerPosition(0, SEEK_MS, "перемотка на паузе не дошла до плеера сессии")
        // Событие перемотки от сессии не возвращает позицию назад: на паузе опроса нет.
        ShadowLooper.idleMainLooper()
        assertEquals(SEEK_MS, controller.positionMs.value)
    }

    @Test
    fun `previous в начале трека - предыдущий трек, в середине - начало текущего`() {
        playReady(1)
        controller.seekTo(SEEK_MS)
        awaitPlayerPosition(1, SEEK_MS, "перемотка не дошла до плеера сессии")

        controller.previous()
        awaitPlayerPosition(1, 0L, "previous дальше порога не вернул в начало текущего трека")

        controller.previous()
        awaitMainLooper("previous в начале трека не перешёл на предыдущий") { player.currentMediaItemIndex == 0 }
        awaitMainLooper("новый индекс не дошёл до состояния контроллера") {
            (controller.state.value as? PlaybackState.Active)?.currentIndex == 0
        }
    }

    @Test
    fun `next доходит до плеера сессии и до состояния`() {
        playReady(0)

        controller.next()

        awaitMainLooper("next не дошёл до плеера сессии") { player.currentMediaItemIndex == 1 }
        awaitMainLooper("новый индекс не дошёл до состояния контроллера") {
            (controller.state.value as? PlaybackState.Active)?.currentIndex == 1
        }
    }

    @Test
    fun `seekBack и seekForward сдвигают позицию плеера сессии на шаг сессии`() {
        playReady(0)
        controller.seekTo(SEEK_MS)
        awaitPlayerPosition(0, SEEK_MS, "перемотка не дошла до плеера сессии")

        controller.seekBack()
        awaitPlayerPosition(0, SEEK_MS - SEEK_BACK_MS, "seekBack не дошёл до плеера сессии")

        controller.seekForward()
        awaitPlayerPosition(0, SEEK_MS - SEEK_BACK_MS + SEEK_FORWARD_MS, "seekForward не дошёл до плеера сессии")
    }

    @Test
    fun `togglePlayPause ставит паузу и снова запускает плеер сессии`() {
        playReady(0)

        controller.togglePlayPause()
        awaitMainLooper("пауза не дошла до плеера сессии") { !player.playWhenReady }
        awaitMainLooper("пауза не дошла до состояния контроллера") {
            (controller.state.value as? PlaybackState.Active)?.playWhenReady == false
        }

        controller.togglePlayPause()
        awaitMainLooper("play не дошёл до плеера сессии") { player.playWhenReady }
    }

    /** Подключение и `play` [index], пока трек не станет перематываемым ([awaitSeekable]). */
    private fun playReady(index: Int) {
        connect()
        controller.play(queue, index)
        awaitSeekable()
        assertEquals(index, player.currentMediaItemIndex)
    }

    private fun awaitPlayerPosition(index: Int, positionMs: Long, message: String) {
        awaitMainLooper("$message: ждали элемент $index и позицию $positionMs") {
            player.currentMediaItemIndex == index && player.currentPosition == positionMs
        }
    }

    /** Ждёт [condition], крутя главный лупер; не дождались — падение с [message], а не голый таймаут. */
    private fun awaitMainLooper(message: String, condition: () -> Boolean) {
        try {
            runMainLooperUntil(condition)
        } catch (e: TimeoutException) {
            fail(
                "$message (индекс ${player.currentMediaItemIndex}, позиция ${player.currentPosition}, " +
                    "состояние плеера ${player.playbackState}, state ${controller.state.value})",
            )
        }
    }

    /** Заменяет процессный жизненный цикл: тест двигает его между CREATED и STARTED. */
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

    private companion object {
        /** Длина окна трека `FakeMediaSourceFactory` по умолчанию. */
        const val FAKE_WINDOW_DURATION_MS = 10_000L
        const val SEEK_MS = 6_000L
        const val SEEK_BACK_MS = 2_000L
        const val SEEK_FORWARD_MS = 3_000L
        const val MAX_SEEK_TO_PREVIOUS_MS = 3_000L
    }
}
