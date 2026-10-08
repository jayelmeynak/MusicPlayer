package com.jayelmeynak.feature.player.impl.service

import android.app.Application
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.test.utils.FakeMediaSourceFactory
import androidx.media3.test.utils.TestExoPlayerBuilder
import androidx.media3.test.utils.robolectric.RobolectricUtil.runMainLooperUntil
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.internal.builders.ServiceComponentBuilder
import dagger.hilt.android.internal.managers.ServiceComponentManager.ServiceComponentBuilderEntryPoint
import dagger.hilt.internal.GeneratedComponent
import dagger.hilt.internal.GeneratedComponentManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
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
 * Hilt is replaced by [PlayBackServiceTestApplication], which injects a test ExoPlayer the way the
 * singleton component would: the same player instance for every service instance.
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
    private lateinit var player: ExoPlayer
    private val controllers = mutableListOf<MediaController>()

    @Before
    fun setUp() {
        application = RuntimeEnvironment.getApplication() as PlayBackServiceTestApplication
        player = TestExoPlayerBuilder(application)
            .setMediaSourceFactory(FakeMediaSourceFactory())
            .build()
        application.player = player
        registerLauncherActivity()
    }

    @After
    fun tearDown() {
        controllers.forEach { it.release() }
        player.release()
    }

    @Test
    fun `сервис создан заново после уничтожения - новая сессия принимает команды`() {
        val first = Robolectric.buildService(PlayBackService::class.java).create()
        val firstSession = first.get().sessions.single()
        first.destroy()

        val second = Robolectric.buildService(PlayBackService::class.java).create()
        val secondSession = second.get().sessions.single()
        val controller = connect(secondSession.token)
        controller.setMediaItem(MediaItem.fromUri("https://example.com/1.mp3"))
        controller.prepare()
        controller.play()
        runMainLooperUntil { player.playWhenReady }

        assertNotSame(firstSession, secondSession)
        assertTrue(controller.isConnected)
        assertEquals(1, player.mediaItemCount)
        second.destroy()
    }

    @Test
    fun `сессия сервиса управляет плеером из графа зависимостей и открывает приложение`() {
        val service = Robolectric.buildService(PlayBackService::class.java).create()

        val session = service.get().sessions.single()

        assertTrue(session.player === player)
        assertNotNull(session.sessionActivity)
        service.destroy()
    }

    @Test
    fun `уничтожение сервиса - сессия убрана, общий плеер остановлен`() {
        val service = Robolectric.buildService(PlayBackService::class.java).create()
        val controller = connect(service.get().sessions.single().token)
        controller.setMediaItem(MediaItem.fromUri("https://example.com/1.mp3"))
        controller.prepare()
        controller.play()
        runMainLooperUntil { player.playWhenReady }

        service.destroy()

        assertTrue(service.get().sessions.isEmpty())
        assertEquals(Player.STATE_IDLE, player.playbackState)
        assertFalse(player.playWhenReady)
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

    private fun connect(token: SessionToken): MediaController {
        val future = MediaController.Builder(application, token).buildAsync()
        runMainLooperUntil { future.isDone }
        return future.get().also { controllers += it }
    }
}

/**
 * Stands in for the Hilt application: hands every [PlayBackService] the shared [player], as the
 * singleton component does, without a generated Hilt test component.
 */
class PlayBackServiceTestApplication : Application(), GeneratedComponentManager<Any> {

    lateinit var player: ExoPlayer

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
            playBackService.exoPlayer = player
        }
    }
}
