package com.jayelmeynak.feature.player.impl.notification

import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.test.utils.FakeMediaSourceFactory
import androidx.media3.test.utils.TestExoPlayerBuilder
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Robolectric provides the notification system, a service to put in the foreground and the
 * main looper the test ExoPlayer and PlayerNotificationManager need.
 */
@RunWith(RobolectricTestRunner::class)
class MusicNotificationManagerTest {

    private lateinit var player: ExoPlayer
    private lateinit var manager: MusicNotificationManager
    private lateinit var service: MediaSessionService

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        player = TestExoPlayerBuilder(context)
            .setMediaSourceFactory(FakeMediaSourceFactory())
            .build()
        manager = MusicNotificationManager(context, player)
        service = Robolectric.buildService(TestService::class.java).create().get()
    }

    @After
    fun tearDown() {
        manager.playerNotificationManager?.setPlayer(null)
        player.release()
    }

    @Test
    fun `повторный старт сервиса не пересобирает уведомление плеера`() {
        manager.startNotificationService(service)
        val first = manager.playerNotificationManager

        manager.startNotificationService(service)

        assertNotNull(first)
        assertSame(first, manager.playerNotificationManager)
    }

    class TestService : MediaSessionService() {
        override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = null
    }
}
