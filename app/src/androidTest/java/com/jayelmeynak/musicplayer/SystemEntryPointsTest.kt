package com.jayelmeynak.musicplayer

import android.app.Activity
import android.app.ActivityOptions
import android.app.PendingIntent
import android.os.Build
import androidx.test.espresso.Espresso
import com.jayelmeynak.musicplayer.screen.MiniPlayerScreen
import com.jayelmeynak.musicplayer.screen.PlayerScreen
import com.jayelmeynak.musicplayer.testing.MusicPlayerTestCase
import com.kaspersky.kaspresso.testcases.core.testcontext.BaseTestContext
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/** Системные точки входа: уведомление, медиакнопки, deep link — одна Activity и текущий трек. */
@HiltAndroidTest
class SystemEntryPointsTest : MusicPlayerTestCase() {

    @Test
    fun `пауза_из_уведомления_ставит_экран_плеера_и_сессию_на_паузу`() = run {
        step("Трек играет, уведомление показано") {
            playFromChart(0)
            awaitNotificationTitle(chart[0].title)
        }
        step("Кнопка паузы уведомления") { tapNotificationAction(PAUSE) }
        step("UI и сессия на паузе") {
            awaitPaused(chart[0].title)
            onComposeScreen<PlayerScreen>(composeRule) { playButton.assertIsDisplayed() }
            awaitSession(STATE_PAUSED, chart[0].title)
        }
    }

    @Test
    fun `next_из_уведомления_при_игре_и_на_паузе_меняет_трек_во_всём_UI`() = run {
        step("Трек 1 играет") {
            playFromChart(0)
            awaitNotificationTitle(chart[0].title)
        }
        step("Next из уведомления при игре") {
            tapNotificationAction(NEXT)
            awaitPlaying(chart[1].title)
            onComposeScreen<PlayerScreen>(composeRule) { title(chart[1].title).assertIsDisplayed() }
            awaitSession(STATE_PLAYING, chart[1].title)
            awaitNotificationTitle(chart[1].title)
        }
        step("Пауза, затем next из уведомления на паузе") {
            tapNotificationAction(PAUSE)
            awaitPaused(chart[1].title)
            tapNotificationAction(NEXT)
            awaitPaused(chart[2].title)
            onComposeScreen<PlayerScreen>(composeRule) { title(chart[2].title).assertIsDisplayed() }
            awaitSession(STATE_PAUSED, chart[2].title)
            awaitNotificationTitle(chart[2].title)
        }
        step("Мини-плеер на списке — трек 3") {
            Espresso.pressBack()
            onComposeScreen<MiniPlayerScreen>(composeRule) { root.assertTextContains(chart[2].title) }
        }
    }

    @Test
    fun `медиакнопки_pause_и_play_управляют_воспроизведением`() = run {
        step("Трек играет") { playFromChart(0) }
        step("KEYCODE_MEDIA_PAUSE") {
            shell("input keyevent KEYCODE_MEDIA_PAUSE")
            awaitPaused(chart[0].title)
            onComposeScreen<PlayerScreen>(composeRule) { playButton.assertIsDisplayed() }
            awaitSession(STATE_PAUSED, chart[0].title)
        }
        step("KEYCODE_MEDIA_PLAY") {
            shell("input keyevent KEYCODE_MEDIA_PLAY")
            awaitPlaying(chart[0].title)
            onComposeScreen<PlayerScreen>(composeRule) { pauseButton.assertIsDisplayed() }
            awaitSession(STATE_PLAYING, chart[0].title)
        }
    }

    @Test
    fun `deep_link_при_игре_и_из_фона__одна_Activity_с_текущим_треком`() = run {
        lateinit var activity: Activity
        step("Трек играет, открыт список") {
            playFromChart(0)
            Espresso.pressBack()
            onComposeScreen<MiniPlayerScreen>(composeRule) { root.assertTextContains(chart[0].title) }
            activity = resumedMainActivities().single()
        }
        step("Deep link при открытом приложении") {
            shell(DEEP_LINK)
            assertSingleActivityWithTrack(activity, chart[0].title)
        }
        step("Deep link из фона после HOME") {
            device.uiDevice.pressHome()
            flakySafely(LOAD_TIMEOUT_MS) { assertEquals(0, resumedMainActivities().size) }
            shell(DEEP_LINK)
            assertSingleActivityWithTrack(activity, chart[0].title)
            awaitPlaying(chart[0].title)
        }
    }

    @Test
    fun `тап_по_уведомлению_из_фона__одна_Activity_с_текущим_треком`() = run {
        lateinit var activity: Activity
        step("Трек играет, открыт список") {
            playFromChart(0)
            Espresso.pressBack()
            activity = resumedMainActivities().single()
            awaitNotificationTitle(chart[0].title)
        }
        step("HOME, затем тап по уведомлению") {
            device.uiDevice.pressHome()
            flakySafely(LOAD_TIMEOUT_MS) { assertEquals(0, resumedMainActivities().size) }
            // Интент тапа по уведомлению — тот же, что отправляет шторка; разметка шторки у OEM разная.
            sendFromBackground(playerNotification().notification.contentIntent)
            assertSingleActivityWithTrack(activity, chart[0].title)
            awaitPlaying(chart[0].title)
        }
    }

    private fun BaseTestContext.assertSingleActivityWithTrack(expected: Activity, title: String) {
        flakySafely(LOAD_TIMEOUT_MS) {
            val resumed = resumedMainActivities()
            assertEquals("MainActivity в RESUMED: $resumed", 1, resumed.size)
            assertSame(expected, resumed.first())
        }
        onComposeScreen<MiniPlayerScreen>(composeRule) { root.assertTextContains(title) }
    }

    /**
     * Отправка интента уведомления из фона. С API 34 отправитель обязан явно разрешить запуск
     * Activity из фона; на API 29–33 хватает обычной отправки.
     */
    private fun sendFromBackground(intent: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val options = ActivityOptions.makeBasic()
                .setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
            intent.send(context, 0, null, null, null, null, options.toBundle())
        } else {
            intent.send()
        }
    }

    private companion object {
        const val PAUSE = "media3_controls_pause_description"
        const val NEXT = "media3_controls_seek_to_next_description"
        const val DEEP_LINK = "am start -W -a android.intent.action.VIEW -d multiplayer://main com.jayelmeynak.musicplayer"
    }
}
