package com.jayelmeynak.feature.player.impl.presentation

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import android.Manifest
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.jayelmeynak.feature.player.api.PlaybackState
import com.jayelmeynak.feature.player.api.PlayerDestination
import com.jayelmeynak.feature.player.impl.R
import com.jayelmeynak.feature.player.api.QueueItem
import com.jayelmeynak.feature.player.api.TrackSource
import com.jayelmeynak.feature.player.api.testing.FakePlaybackController
import com.jayelmeynak.lib.navigation.testing.FakeNavigator
import com.jayelmeynak.util.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

/** Robolectric — Compose-правило с тестовой Activity, запущенные сервисы и запросы разрешений (API 34). */
@RunWith(RobolectricTestRunner::class)
class PlayerRouteTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val promptStore = FakeNotificationPromptStore()

    private val navigator = FakeNavigator()

    @Test
    fun `подключён с пустой очередью - экран закрывается один раз`() {
        val controller = FakePlaybackController()

        setPlayerRoute(controller)
        // Второй пустой подключённый снапшот (другое значение — StateFlow не отбросит его как повтор).
        controller.state.value = (controller.state.value as PlaybackState.Active).copy(isPlaying = true)
        composeRule.waitForIdle()

        // Экран убирает свой ключ, а не «назад»: поверх него мог открыться другой экран.
        assertEquals(listOf(PlayerDestination), navigator.removed)
        assertEquals(0, navigator.backCount)
    }

    @Test
    fun `подключиться не удалось - текст ошибки вместо индикатора`() {
        val controller = FakePlaybackController(connected = false)
        setPlayerRoute(controller)

        controller.failConnection()

        composeRule.onNodeWithText(RuntimeEnvironment.getApplication().getString(R.string.player_error_connection)).assertExists()
        assertEquals(emptyList<Any>(), navigator.removed)
    }

    @Test
    fun `до подключения экран ждёт с индикатором, не закрывается`() {
        setPlayerRoute(FakePlaybackController(connected = false))

        composeRule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo.Indeterminate))
            .assertExists()
        composeRule.onNodeWithContentDescription("Воспроизведение").assertDoesNotExist()
        assertEquals(0, navigator.backCount)
        assertEquals(emptyList<Any>(), navigator.removed)
    }

    @Test
    fun `с треком - экран показывает его, не закрывается и не стартует сервис`() {
        val controller = FakePlaybackController().apply {
            play(listOf(QueueItem("42", TrackSource.DEEZER, "Remote 42", "Artist", null, 0L)), 0)
        }

        setPlayerRoute(controller)

        composeRule.onNodeWithText("Remote 42").assertExists()
        assertEquals(0, navigator.backCount)
        assertEquals(emptyList<Any>(), navigator.removed)
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

    @Test
    fun `первый показ без разрешения уведомлений - системный запрос`() {
        setPlayerRoute(FakePlaybackController())

        val request = shadowOf(composeRule.activity).lastRequestedPermission
        assertTrue(request?.requestedPermissions.orEmpty().contains(Manifest.permission.POST_NOTIFICATIONS))
        assertTrue(promptStore.prompted)
    }

    @Test
    fun `разрешение уведомлений есть - запроса нет`() {
        shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        setPlayerRoute(FakePlaybackController())

        assertNull(shadowOf(composeRule.activity).lastRequestedPermission)
    }

    private fun setPlayerRoute(controller: FakePlaybackController) {
        val viewModel = PlayerViewModel(controller, promptStore)
        composeRule.setContent { PlayerRoute(navigator = navigator, viewModel = viewModel) }
        composeRule.waitForIdle()
    }
}
