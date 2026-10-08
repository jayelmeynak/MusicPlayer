package com.jayelmeynak.feature.player.impl.presentation

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation3.runtime.NavKey
import com.jayelmeynak.feature.player.api.PlayerDestination
import com.jayelmeynak.feature.player.api.QueueItem
import com.jayelmeynak.feature.player.api.TrackSource
import com.jayelmeynak.feature.player.api.testing.FakePlaybackController
import com.jayelmeynak.feature.player.api.testing.FakePlaybackController.Command
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Robolectric hosts the Compose test rule. */
@RunWith(RobolectricTestRunner::class)
class MiniPlayerTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val navigator = FakeNavigator()

    @Test
    fun `клик по мини-плееру открывает экран плеера`() {
        val controller = FakePlaybackController().apply { play(listOf(track()), 0) }
        setMiniPlayer(controller)

        composeRule.onNodeWithText("Current song").performClick()

        assertEquals(listOf<NavKey>(PlayerDestination), navigator.destinations)
    }

    @Test
    fun `пустая очередь - мини-плеер не показан`() {
        setMiniPlayer(FakePlaybackController())

        composeRule.onNodeWithTag(TAG).assertDoesNotExist()
        composeRule.onAllNodes(hasClickAction()).assertCountEquals(0)
    }

    @Test
    fun `контроллер не подключён - мини-плеер не показан, после подключения показан`() {
        val controller = FakePlaybackController(connected = false).apply { play(listOf(track()), 0) }
        setMiniPlayer(controller)
        composeRule.onNodeWithTag(TAG).assertDoesNotExist()

        controller.connect()

        composeRule.onNodeWithTag(TAG).assertExists()
    }

    @Test
    fun `кнопка паузы уходит в контроллер`() {
        val controller = FakePlaybackController().apply { play(listOf(track()), 0) }
        setMiniPlayer(controller)

        composeRule.onNodeWithContentDescription("Пауза").performClick()

        assertEquals(Command.TogglePlayPause, controller.commands.last())
    }

    private fun setMiniPlayer(controller: FakePlaybackController) {
        composeRule.setContent {
            MiniPlayer(
                playbackController = controller,
                navigator = navigator,
                modifier = Modifier.testTag(TAG),
            )
        }
    }

    private fun track() = QueueItem("7", TrackSource.DEEZER, "Current song", "Artist", null, 0L)

    private companion object {
        const val TAG = "mini_player"
    }
}
