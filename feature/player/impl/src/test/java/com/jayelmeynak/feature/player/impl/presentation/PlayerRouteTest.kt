package com.jayelmeynak.feature.player.impl.presentation

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.jayelmeynak.feature.player.api.PlaybackState
import com.jayelmeynak.feature.player.api.QueueItem
import com.jayelmeynak.feature.player.api.TrackSource
import com.jayelmeynak.feature.player.api.testing.FakePlaybackController
import com.jayelmeynak.feature.player.impl.artwork.FakeLocalArtworkSource
import com.jayelmeynak.lib.navigation.testing.FakeNavigator
import com.jayelmeynak.util.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

/** Robolectric hosts the Compose test rule and records started services. */
@RunWith(RobolectricTestRunner::class)
class PlayerRouteTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    @get:Rule
    val composeRule = createComposeRule()

    private val navigator = FakeNavigator()

    @Test
    fun `подключён с пустой очередью - экран закрывается один раз`() {
        val controller = FakePlaybackController()

        setPlayerRoute(controller)
        controller.state.value = controller.state.value
        composeRule.waitForIdle()

        assertEquals(1, navigator.backCount)
    }

    @Test
    fun `до подключения экран ждёт, не закрывается`() {
        setPlayerRoute(FakePlaybackController(connected = false))

        assertEquals(0, navigator.backCount)
    }

    @Test
    fun `с треком - экран показывает его и не стартует сервис`() {
        val controller = FakePlaybackController().apply {
            play(listOf(QueueItem("42", TrackSource.DEEZER, "Remote 42", "Artist", null, 0L)), 0)
        }

        setPlayerRoute(controller)

        composeRule.onNodeWithText("Remote 42").assertExists()
        assertEquals(0, navigator.backCount)
        assertNull(shadowOf(RuntimeEnvironment.getApplication()).nextStartedService)
    }

    @Test
    fun `буферизация при запрошенном воспроизведении - кнопка паузы и индикатор загрузки`() {
        val controller = FakePlaybackController().apply {
            play(listOf(QueueItem("42", TrackSource.DEEZER, "Remote 42", "Artist", null, 0L)), 0)
            state.value = (state.value as PlaybackState.Active)
                .copy(isPlaying = false, playWhenReady = true, isBuffering = true, showPlayButton = false)
        }

        setPlayerRoute(controller)

        composeRule.onNodeWithContentDescription("Пауза").assertExists()
        composeRule.onNodeWithContentDescription("Воспроизведение").assertDoesNotExist()
        composeRule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo.Indeterminate))
            .assertExists()
    }

    @Test
    fun `пауза - кнопка воспроизведения без индикатора загрузки`() {
        val controller = FakePlaybackController().apply {
            play(listOf(QueueItem("42", TrackSource.DEEZER, "Remote 42", "Artist", null, 0L)), 0)
            state.value = (state.value as PlaybackState.Active)
                .copy(isPlaying = false, playWhenReady = false, showPlayButton = true)
        }

        setPlayerRoute(controller)

        composeRule.onNodeWithContentDescription("Воспроизведение").assertExists()
        composeRule.onNodeWithContentDescription("Пауза").assertDoesNotExist()
        composeRule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo.Indeterminate))
            .assertDoesNotExist()
    }

    private fun setPlayerRoute(controller: FakePlaybackController) {
        val viewModel = PlayerViewModel(controller, FakeLocalArtworkSource())
        composeRule.setContent { PlayerRoute(navigator = navigator, viewModel = viewModel) }
        composeRule.waitForIdle()
    }
}
