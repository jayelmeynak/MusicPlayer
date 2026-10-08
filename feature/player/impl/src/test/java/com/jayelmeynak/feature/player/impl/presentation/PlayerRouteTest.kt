package com.jayelmeynak.feature.player.impl.presentation

import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.test.utils.FakeMediaSourceFactory
import androidx.media3.test.utils.TestExoPlayerBuilder
import com.jayelmeynak.feature.player.api.TrackSource
import com.jayelmeynak.feature.player.impl.domain.models.Track
import com.jayelmeynak.feature.player.impl.navigation.PlayerRequestHolder
import com.jayelmeynak.feature.player.impl.service.MusicServiceHandler
import com.jayelmeynak.feature.player.impl.service.PlayBackService
import com.jayelmeynak.util.testing.MainDispatcherRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

/**
 * Robolectric hosts the Compose test rule, records started services and runs the test ExoPlayer
 * behind the real AudioViewModel (see AudioViewModelTest).
 */
@RunWith(RobolectricTestRunner::class)
class PlayerRouteTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    @get:Rule
    val composeRule = createComposeRule()

    private val scope = CoroutineScope(SupervisorJob())
    private val navigator = FakeNavigator()
    private val requests = PlayerRequestHolder()
    private val remoteRepository = FakeMusicRemoteRepository()
    private lateinit var player: ExoPlayer
    private lateinit var handler: MusicServiceHandler

    @Before
    fun setUp() {
        player = TestExoPlayerBuilder(RuntimeEnvironment.getApplication())
            .setMediaSourceFactory(FakeMediaSourceFactory())
            .build()
        handler = MusicServiceHandler(player, scope, RuntimeEnvironment.getApplication())
    }

    @After
    fun tearDown() {
        player.release()
        scope.cancel()
    }

    @Test
    fun `без запроса и с пустой очередью - экран закрывается, сервис не стартует`() {
        setPlayerRoute()

        assertEquals(1, navigator.backCount)
        assertNull(shadowOf(RuntimeEnvironment.getApplication()).nextStartedService)
    }

    @Test
    fun `с запросом - сервис воспроизведения стартует, экран остаётся`() {
        remoteRepository.tracks = mapOf("42" to remoteTrack(42))
        requests.open(TrackSource.DEEZER, "42")

        setPlayerRoute()

        assertEquals(0, navigator.backCount)
        val started = shadowOf(RuntimeEnvironment.getApplication()).nextStartedService
        assertEquals(PlayBackService::class.java.name, started.component?.className)
    }

    @Test
    fun `с запросом - сервис стартует обычным startService, в foreground его переводит Media3`() {
        remoteRepository.tracks = mapOf("42" to remoteTrack(42))
        requests.open(TrackSource.DEEZER, "42")
        val context = RecordingContext(RuntimeEnvironment.getApplication())

        setPlayerRoute(context)

        assertEquals(listOf("startService"), context.calls)
    }

    private fun setPlayerRoute(context: Context) {
        val viewModel = testAudioViewModel(handler, requests, remoteRepository)
        composeRule.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                PlayerRoute(viewModel = viewModel, navigator = navigator)
            }
        }
        composeRule.waitForIdle()
    }

    /** Records how the route starts the service; the calls still reach Robolectric. */
    private class RecordingContext(base: Context) : ContextWrapper(base) {
        val calls = mutableListOf<String>()

        override fun startService(service: Intent): ComponentName? {
            calls += "startService"
            return super.startService(service)
        }

        override fun startForegroundService(service: Intent): ComponentName? {
            calls += "startForegroundService"
            return super.startForegroundService(service)
        }
    }

    private fun setPlayerRoute() {
        val viewModel = testAudioViewModel(handler, requests, remoteRepository)
        composeRule.setContent { PlayerRoute(viewModel = viewModel, navigator = navigator) }
        composeRule.waitForIdle()
    }

    private fun remoteTrack(id: Long) = Track(
        id = id,
        title = "Remote $id",
        album = null,
        artistName = "Artist $id",
        preview = "https://example.com/$id.mp3",
        uri = null,
    )
}
