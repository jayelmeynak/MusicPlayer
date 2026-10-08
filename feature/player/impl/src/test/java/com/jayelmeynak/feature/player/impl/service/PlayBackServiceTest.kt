package com.jayelmeynak.feature.player.impl.service

import android.app.Application
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.test.utils.FakeMediaSourceFactory
import androidx.media3.test.utils.TestExoPlayerBuilder
import androidx.media3.test.utils.robolectric.RobolectricUtil.runMainLooperUntil
import com.jayelmeynak.feature.player.api.QueueItem
import com.jayelmeynak.feature.player.api.TrackSource
import com.jayelmeynak.feature.player.impl.artwork.LocalArtworkBitmapLoader
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.internal.builders.ServiceComponentBuilder
import dagger.hilt.android.internal.managers.ServiceComponentManager.ServiceComponentBuilderEntryPoint
import dagger.hilt.internal.GeneratedComponent
import dagger.hilt.internal.GeneratedComponentManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertEquals
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
    fun `старт сервиса удаляет канал уведомлений прежней версии приложения`() {
        val notifications = NotificationManagerCompat.from(application)
        notifications.createNotificationChannel(
            NotificationChannelCompat.Builder(LEGACY_CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName("notification channel 1")
                .build()
        )

        Robolectric.buildService(PlayBackService::class.java).create().destroy()

        assertNull(notifications.getNotificationChannel(LEGACY_CHANNEL_ID))
    }

    /** The session activity is the package launch intent; the test manifest has no launcher. */
    private fun registerLauncherActivity() {
        val component = ComponentName(application, "${application.packageName}.Launcher")
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
            playBackService.sessionCallback = PlaybackSessionCallback()
            playBackService.bitmapLoader = LocalArtworkBitmapLoader(
                context = this@PlayBackServiceTestApplication,
                artworkSource = { null },
                scope = CoroutineScope(Dispatchers.Unconfined),
                ioDispatcher = Dispatchers.Unconfined,
            )
        }
    }
}

/** Channel id of the notification the app built by hand before the standard Media3 one. */
private const val LEGACY_CHANNEL_ID = "notification channel id 1"
