package com.jayelmeynak.feature.player.impl.service

import android.app.Application
import android.app.ForegroundServiceStartNotAllowedException
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.view.KeyEvent
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.test.utils.FakeMediaSourceFactory
import androidx.media3.test.utils.TestExoPlayerBuilder
import androidx.media3.test.utils.robolectric.RobolectricUtil.runMainLooperUntil
import com.jayelmeynak.feature.player.api.OpenPlayerRequest
import com.jayelmeynak.feature.player.api.QueueItem
import com.jayelmeynak.feature.player.api.TrackSource
import com.jayelmeynak.feature.player.api.key
import com.jayelmeynak.feature.player.impl.persistence.FakePlaybackStateStore
import com.jayelmeynak.feature.player.impl.persistence.PlaybackPersistence
import com.jayelmeynak.feature.player.impl.persistence.PlaybackResumption
import com.jayelmeynak.feature.player.impl.persistence.SavedPosition
import com.jayelmeynak.feature.player.impl.persistence.SavedQueue
import com.jayelmeynak.feature.player.impl.persistence.SavedQueueItem
import com.jayelmeynak.feature.player.impl.persistence.toSaved
import com.jayelmeynak.feature.player.impl.artwork.ContentArtworkBitmapLoader
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.internal.builders.ServiceComponentBuilder
import dagger.hilt.android.internal.managers.ServiceComponentManager.ServiceComponentBuilderEntryPoint
import dagger.hilt.internal.GeneratedComponent
import dagger.hilt.internal.GeneratedComponentManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import java.io.FileNotFoundException

/**
 * Robolectric runs the real MediaSessionService and an in-process MediaController against it.
 * Hilt is replaced by [PlayBackServiceTestApplication], which injects a player factory that builds
 * test ExoPlayers and records them.
 *
 * The replacement relies on Hilt internals, checked with Hilt 2.59.2: the generated
 * `PlayBackService_GeneratedInjector`, `dagger.hilt.android.internal.builders.ServiceComponentBuilder`
 * and `ServiceComponentManager.ServiceComponentBuilderEntryPoint`. A Hilt update that changes them
 * breaks compilation or [Robolectric.buildService] here, not the app.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = PlayBackServiceTestApplication::class)
class PlayBackServiceTest {

    private lateinit var application: PlayBackServiceTestApplication
    private val controllers = mutableListOf<MediaController>()

    @Before
    fun setUp() {
        application = RuntimeEnvironment.getApplication() as PlayBackServiceTestApplication
        application.players.clear()
        application.store = FakePlaybackStateStore()
        registerLauncherActivity()
    }

    @After
    fun tearDown() {
        controllers.forEach { it.release() }
        application.players.forEach { it.release() }
    }

    @Test
    fun `сервис создан заново после уничтожения - новые плеер и сессия принимают команды`() {
        val first = Robolectric.buildService(PlayBackService::class.java).create()
        val firstSession = first.get().sessions.single()
        first.destroy()

        val second = Robolectric.buildService(PlayBackService::class.java).create()
        val secondSession = second.get().sessions.single()
        val controller = connect(secondSession.token)
        controller.setMediaItem(queueItem.toMediaItem())
        controller.prepare()
        controller.play()
        val player = application.players.last()
        runMainLooperUntil { player.playWhenReady }

        assertNotSame(firstSession, secondSession)
        assertEquals(2, application.players.size)
        assertNotSame(application.players[0], player)
        assertTrue(controller.isConnected)
        assertEquals(1, player.mediaItemCount)
        second.destroy()
    }

    @Test
    fun `сессия сервиса управляет его собственным плеером и открывает приложение`() {
        val service = Robolectric.buildService(PlayBackService::class.java).create()

        val session = service.get().sessions.single()

        assertTrue(session.player === application.players.single())
        assertNotNull(session.sessionActivity)
        service.destroy()
    }

    @Test
    fun `тап по уведомлению - неизменяемый явный интент launcher-активности с действием открыть плеер, без extras`() {
        val service = Robolectric.buildService(PlayBackService::class.java).create()

        val sessionActivity = checkNotNull(service.get().sessions.single().sessionActivity)
        val shadow = shadowOf(sessionActivity)
        val intent = shadow.savedIntent

        assertTrue(sessionActivity.isImmutable)
        assertTrue(shadow.isActivity)
        assertEquals(OpenPlayerRequest.ACTION, intent.action)
        assertEquals(launcherComponent(), intent.component)
        assertEquals(
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP,
            intent.flags and (Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )
        assertNull(intent.extras)
        assertNull(intent.categories)
        assertNull(intent.data)
        service.destroy()
    }

    @Test
    fun `элемент от контроллера приходит в плеер с внутренним URI, без URL`() {
        val service = Robolectric.buildService(PlayBackService::class.java).create()
        val controller = connect(service.get().sessions.single().token)

        controller.setMediaItem(queueItem.toMediaItem())
        val player = application.players.single()
        runMainLooperUntil { player.mediaItemCount == 1 }

        val uri = player.getMediaItemAt(0).localConfiguration?.uri
        assertEquals(TrackKey(TrackSource.DEEZER, "42"), uri?.toTrackKey())
        service.destroy()
    }

    /**
     * End to end through the real service and its callback. Under Robolectric an in-process
     * controller is also `isTrusted`, so this does not tell the own-uid branch of the access policy
     * from the trusted one; that branch is covered by `PlaybackSessionCallbackTest`.
     */
    @Test
    fun `контроллер своего приложения подключается к сервису и читает очередь`() {
        val service = Robolectric.buildService(PlayBackService::class.java).create()
        val controller = connect(service.get().sessions.single().token)

        controller.setMediaItem(queueItem.toMediaItem())
        runMainLooperUntil { controller.mediaItemCount == 1 }

        assertTrue(controller.isCommandAvailable(Player.COMMAND_GET_TIMELINE))
        assertEquals(queueItem, controller.getMediaItemAt(0).toQueueItem())
        service.destroy()
    }

    @Test
    fun `уничтожение сервиса - сессия убрана, плеер освобождён`() {
        val service = Robolectric.buildService(PlayBackService::class.java).create()
        val controller = connect(service.get().sessions.single().token)
        controller.setMediaItem(queueItem.toMediaItem())
        controller.prepare()
        controller.play()
        val player = application.players.single()
        runMainLooperUntil { player.playWhenReady }

        service.destroy()

        assertTrue(service.get().sessions.isEmpty())
        assertTrue(player.isReleased)
    }

    @Test
    fun `новая сессия поднимает сохранённую очередь на паузе, без звука`() {
        application.store = savedStore()

        val service = Robolectric.buildService(PlayBackService::class.java).create()
        val session = service.get().sessions.single()
        val player = application.players.single()
        runMainLooperUntil { !session.sessionExtras.getBoolean(EXTRA_SESSION_RESTORING, true) }

        assertEquals(savedQueue, List(player.mediaItemCount) { player.getMediaItemAt(it).toQueueItem() })
        assertEquals(1, player.currentMediaItemIndex)
        assertEquals(42_000L, player.currentPosition)
        assertFalse(player.playWhenReady)
        assertEquals(Player.STATE_IDLE, player.playbackState)
        assertTrue(List(player.mediaItemCount) { player.getMediaItemAt(it) }.all { it.localConfiguration?.uri?.toTrackKey() != null })
        service.destroy()
    }

    @Test
    fun `сохранённой очереди нет - сессия пуста и сообщает, что восстановление завершено`() {
        val service = Robolectric.buildService(PlayBackService::class.java).create()
        val session = service.get().sessions.single()

        runMainLooperUntil { !session.sessionExtras.getBoolean(EXTRA_SESSION_RESTORING, true) }

        assertEquals(0, application.players.single().mediaItemCount)
        service.destroy()
    }

    @Test
    fun `очередь, заданная до конца восстановления, не перетирается сохранённой`() {
        application.store = savedStore()
        application.store.readGate = true
        val service = Robolectric.buildService(PlayBackService::class.java).create()
        val session = service.get().sessions.single()
        val controller = connect(session.token)
        assertTrue(controller.sessionExtras.getBoolean(EXTRA_SESSION_RESTORING))

        controller.setMediaItem(queueItem.toMediaItem())
        val player = application.players.single()
        runMainLooperUntil { player.mediaItemCount == 1 }
        application.store.openGate()
        runMainLooperUntil { !session.sessionExtras.getBoolean(EXTRA_SESSION_RESTORING, true) }

        assertEquals(listOf(queueItem), List(player.mediaItemCount) { player.getMediaItemAt(it).toQueueItem() })
        service.destroy()
    }

    @Test
    fun `медиакнопка play после смерти процесса - играет сохранённый трек с сохранённой позиции`() {
        application.store = savedStore()

        val service = Robolectric.buildService(PlayBackService::class.java, mediaButton(KeyEvent.KEYCODE_MEDIA_PLAY))
            .create()
            .startCommand(0, 1)
        val player = application.players.single()
        runMainLooperUntil { player.playWhenReady }

        assertEquals(savedQueue, List(player.mediaItemCount) { player.getMediaItemAt(it).toQueueItem() })
        assertEquals(1, player.currentMediaItemIndex)
        assertTrue(player.currentPosition >= 42_000L)
        service.destroy()
    }

    @Test
    fun `медиакнопка, а очередь повреждена - сервис останавливается, не висит в foreground`() {
        // Повреждённый файл DataStore заменяет пустой очередью: хранилище отдаёт null.
        application.store = FakePlaybackStateStore(queue = null)

        assertStopsWithoutPlayback()
    }

    @Test
    fun `медиакнопка, а источники очереди неизвестны - сервис останавливается, не висит в foreground`() {
        application.store = FakePlaybackStateStore(
            queue = SavedQueue(listOf(SavedQueueItem("1", "GONE", "One", "A", null, 0L))),
            position = SavedPosition(index = 0, key = "GONE|1", positionMs = 0L),
        )

        assertStopsWithoutPlayback()
    }

    @Test
    fun `медиакнопка из уведомления работающего сервиса и неудачное возобновление - сервис не останавливается`() {
        // Первый старт — свой (Media3 уводит сервис в foreground), медиакнопка уведомления — следующий.
        val service = Robolectric.buildService(PlayBackService::class.java, Intent(application, PlayBackService::class.java))
            .create()
            .startCommand(0, 1)
        val shadow = shadowOf(service.get())
        try {
            service.withIntent(mediaButton(KeyEvent.KEYCODE_MEDIA_PLAY)).startCommand(0, 2)
            repeat(5) { ShadowLooper.idleMainLooper() }

            assertFalse(shadow.isStoppedBySelf)
            assertNull(shadow.lastForegroundNotification)
        } finally {
            service.destroy()
        }
    }

    @Test
    fun `первый старт медиакнопкой, foreground запрещён, возобновлять нечего - сервис всё равно останавливается`() {
        val service = Robolectric.buildService(PlayBackService::class.java, mediaButton(KeyEvent.KEYCODE_MEDIA_PLAY)).create()
        val shadow = shadowOf(service.get())
        shadow.setThrowInStartForeground(ForegroundServiceStartNotAllowedException("запрещено в тесте"))
        try {
            service.startCommand(0, 1)

            runMainLooperUntil { shadow.isStoppedBySelf }
        } finally {
            service.destroy()
        }
    }

    /** Сервис запущен приёмником как foreground: без воспроизведения он обязан остановиться сам. */
    private fun assertStopsWithoutPlayback() {
        val service = Robolectric.buildService(PlayBackService::class.java, mediaButton(KeyEvent.KEYCODE_MEDIA_PLAY))
            .create()
            .startCommand(0, 1)
        val shadow = shadowOf(service.get())
        try {
            runMainLooperUntil { shadow.isStoppedBySelf }

            assertTrue(shadow.isForegroundStopped)
            assertTrue(shadow.notificationShouldRemoved)
            assertEquals(0, application.players.single().mediaItemCount)
        } finally {
            service.destroy()
        }
    }

    @Test
    fun `при игре сервис в foreground с уведомлением`() {
        val service = Robolectric.buildService(PlayBackService::class.java).create()
        val controller = connect(service.get().sessions.single().token)

        controller.setMediaItem(queueItem.toMediaItem())
        controller.prepare()
        controller.play()

        runMainLooperUntil { shadowOf(service.get()).lastForegroundNotification != null }
        service.destroy()
    }

    @Test
    fun `запрет foreground-старта ставит воспроизведение на паузу`() {
        val service = Robolectric.buildService(PlayBackService::class.java).create()
        shadowOf(service.get()).setThrowInStartForeground(ForegroundServiceStartNotAllowedException("запрещено в тесте"))
        val controller = connect(service.get().sessions.single().token)
        val player = application.players.single()

        controller.setMediaItem(queueItem.toMediaItem())
        controller.prepare()
        controller.play()
        runMainLooperUntil { player.playWhenReady }

        runMainLooperUntil { !player.playWhenReady }
        assertTrue(shadowOf(service.get()).lastForegroundNotification == null)
        service.destroy()
    }

    @Test
    fun `сервис сохраняет очередь, а при уничтожении - позицию`() {
        val service = Robolectric.buildService(PlayBackService::class.java).create()
        val controller = connect(service.get().sessions.single().token)
        controller.setMediaItem(queueItem.toMediaItem())
        val player = application.players.single()
        runMainLooperUntil { player.mediaItemCount == 1 }

        service.destroy()
        runMainLooperUntil { application.store.savedQueues.isNotEmpty() }

        assertEquals(listOf(queueItem.toSaved()), application.store.savedQueues.last().items)
        assertEquals(queueItem.key, application.store.savedPositions.last().key)
    }

    private val savedQueue = listOf(
        QueueItem("1", TrackSource.DEEZER, "One", "A", null, 30_000L),
        QueueItem("2", TrackSource.DEEZER, "Two", "B", null, 30_000L),
    )

    private fun savedStore() = FakePlaybackStateStore(
        queue = SavedQueue(savedQueue.map { it.toSaved() }),
        position = SavedPosition(index = 1, key = savedQueue[1].key, positionMs = 42_000L),
    )

    private fun mediaButton(keyCode: Int) = Intent(Intent.ACTION_MEDIA_BUTTON)
        .setClass(application, PlayBackService::class.java)
        .putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_DOWN, keyCode))

    private fun launcherComponent() = ComponentName(application, "${application.packageName}.Launcher")

    /** В тестовом манифесте нет launcher-активности: регистрирует её, `sessionActivity` берёт её компонент. */
    private fun registerLauncherActivity() {
        val component = launcherComponent()
        shadowOf(application.packageManager).apply {
            addActivityIfNotPresent(component)
            addIntentFilterForActivity(
                component,
                IntentFilter(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) },
            )
        }
    }

    private val queueItem = QueueItem("42", TrackSource.DEEZER, "Title", "Artist", null, 0L)

    private fun connect(token: SessionToken): MediaController {
        val future = MediaController.Builder(application, token).buildAsync()
        runMainLooperUntil { future.isDone }
        return future.get().also { controllers += it }
    }
}

/**
 * Stands in for the Hilt application: hands every [PlayBackService] a factory of test players
 * (recorded in [players]), the real session callback and a bitmap loader, without a generated
 * Hilt test component.
 */
class PlayBackServiceTestApplication : Application(), GeneratedComponentManager<Any> {

    val players = mutableListOf<ExoPlayer>()

    /** Хранилище сохранённого воспроизведения для следующего созданного сервиса. */
    internal var store = FakePlaybackStateStore()

    private val scope = CoroutineScope(SupervisorJob())

    override fun generatedComponent(): Any = SingletonComponent()

    private inner class SingletonComponent : GeneratedComponent, ServiceComponentBuilderEntryPoint {
        override fun serviceComponentBuilder(): ServiceComponentBuilder = ServiceBuilder()
    }

    private inner class ServiceBuilder : ServiceComponentBuilder {
        override fun service(service: Service): ServiceComponentBuilder = this

        override fun build(): ServiceComponent = Injector()
    }

    private inner class Injector : ServiceComponent, PlayBackService_GeneratedInjector {
        override fun injectPlayBackService(playBackService: PlayBackService) {
            playBackService.playerFactory = PlayerFactory {
                TestExoPlayerBuilder(this@PlayBackServiceTestApplication)
                    .setMediaSourceFactory(FakeMediaSourceFactory())
                    .build()
                    .also { players += it }
            }
            playBackService.sessionCallback = testSessionCallback(store)
            playBackService.persistence = PlaybackPersistence(store, scope, Dispatchers.Main.immediate)
            playBackService.restorer = SessionRestorer(PlaybackResumption(store), scope, Dispatchers.Main.immediate)
            playBackService.bitmapLoader = ContentArtworkBitmapLoader(
                context = this@PlayBackServiceTestApplication,
                thumbnails = { uri, _ -> throw FileNotFoundException(uri.toString()) },
                scope = CoroutineScope(Dispatchers.Unconfined),
                ioDispatcher = Dispatchers.Unconfined,
            )
        }
    }
}
