package com.jayelmeynak.feature.player.impl.presentation

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.test.utils.FakeMediaSourceFactory
import androidx.media3.test.utils.TestExoPlayerBuilder
import androidx.navigation3.runtime.NavKey
import com.jayelmeynak.feature.player.api.PlayerDestination
import com.jayelmeynak.feature.player.impl.service.MusicServiceHandler
import com.jayelmeynak.util.testing.MainDispatcherRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Robolectric hosts the Compose test rule and the test ExoPlayer behind the real AudioViewModel
 * (see AudioViewModelTest). The player looper never runs, so nothing starts playing.
 */
@RunWith(RobolectricTestRunner::class)
class MiniPlayerTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    @get:Rule
    val composeRule = createComposeRule()

    private val scope = CoroutineScope(SupervisorJob())
    private val navigator = FakeNavigator()
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
    fun `клик по мини-плееру открывает экран плеера без аргументов`() {
        handler.setMediaItemList(listOf(mediaItem(7, "Current song")))
        setMiniPlayer()

        composeRule.onNodeWithText("Current song").performClick()

        assertEquals(listOf<NavKey>(PlayerDestination), navigator.destinations)
    }

    @Test
    fun `пустая очередь - мини-плеер не показан`() {
        setMiniPlayer()

        composeRule.onNodeWithTag(TAG).assertDoesNotExist()
        composeRule.onAllNodes(hasClickAction()).assertCountEquals(0)
    }

    @Test
    fun `непустая очередь - мини-плеер показан`() {
        handler.setMediaItemList(listOf(mediaItem(7, "Current song")))
        setMiniPlayer()

        composeRule.onNodeWithTag(TAG).assertExists()
    }

    private fun setMiniPlayer() {
        val viewModel = testAudioViewModel(handler)
        composeRule.setContent {
            MiniPlayer(viewModel = viewModel, navigator = navigator, modifier = Modifier.testTag(TAG))
        }
    }

    private fun mediaItem(id: Long, title: String): MediaItem = MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri("https://example.com/$id.mp3")
        .setMediaMetadata(MediaMetadata.Builder().setTitle(title).build())
        .build()

    private companion object {
        const val TAG = "mini_player"
    }
}
