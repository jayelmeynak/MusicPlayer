package com.jayelmeynak.musicplayer

import android.content.ComponentName
import android.content.Intent
import androidx.test.espresso.Espresso
import com.jayelmeynak.musicplayer.screen.MiniPlayerScreen
import com.jayelmeynak.musicplayer.testing.MusicPlayerTestCase
import com.jayelmeynak.musicplayer.testing.SessionDump
import com.kaspersky.kaspresso.testcases.core.testcontext.BaseTestContext
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Возобновление после остановки сервиса плеера. Процесс теста — процесс приложения, убить его
 * (`am kill`) из инструментации нельзя: сервис останавливается в том же процессе, после ухода
 * приложения в фон (контроллер UI отвязан). Новая сессия восстанавливает очередь из хранилища —
 * тот же путь, что после смерти процесса; `am kill` и гарнитура проверяются вручную.
 *
 * Требование к устройству для медиакнопки: никакое другое медиаприложение не играет и не играло
 * после этого теста — `input keyevent KEYCODE_MEDIA_PLAY` система отдаёт сессии медиакнопок или
 * последнему приёмнику медиакнопок. Тест проверяет адресата по `dumpsys media_session` до нажатия и
 * падает с понятным текстом, если клавиша ушла бы чужому пакету.
 */
@HiltAndroidTest
class ResumptionTest : MusicPlayerTestCase() {

    @Test
    fun `после_остановки_сервиса_мини_плеер_показывает_последний_трек_на_паузе`() = run {
        val title = chart[2].title
        step("Трек играет, пауза, открыт список") { playPauseAndOpenList(2) }
        step("Приложение в фоне, сервис плеера остановлен") { stopPlaybackServiceInBackground() }
        step("Возврат в приложение: последний трек на паузе в мини-плеере") {
            shell(DEEP_LINK)
            flakySafely(PLAYBACK_TIMEOUT_MS) {
                val state = active()
                assertTrue("снапшот не подключён: $state", state.isConnected)
                assertEquals(title, state.current?.title)
                assertFalse("играет без тапа: $state", state.isPlaying)
            }
            onComposeScreen<MiniPlayerScreen>(composeRule) { root.assertTextContains(title) }
        }
        step("Play продолжает тот же трек") {
            onMain { controller().togglePlayPause() }
            awaitPlaying(title)
        }
    }

    @Test
    fun `медиакнопка_play_после_остановки_сервиса_возобновляет_последний_трек`() = run {
        val title = chart[1].title
        step("Трек играет, пауза, открыт список") { playPauseAndOpenList(1) }
        step("Приложение в фоне, сервис плеера остановлен") { stopPlaybackServiceInBackground() }
        step("Медиакнопка достанется нашему приложению") {
            flakySafely(LOAD_TIMEOUT_MS) {
                val dump = shell("dumpsys media_session")
                assertEquals("адресат медиакнопки — другое приложение:\n$dump", context.packageName, SessionDump.mediaButtonTarget(dump))
            }
        }
        step("KEYCODE_MEDIA_PLAY из фона: сервис стартует и играет последний трек") {
            shell("input keyevent KEYCODE_MEDIA_PLAY")
            awaitSession(STATE_PLAYING, title)
        }
        step("Возврат в приложение: тот же трек играет") {
            shell(DEEP_LINK)
            awaitPlaying(title)
            onComposeScreen<MiniPlayerScreen>(composeRule) { root.assertTextContains(title) }
        }
    }

    private fun BaseTestContext.playPauseAndOpenList(index: Int) {
        playFromChart(index)
        onMain { controller().togglePlayPause() }
        awaitPaused(chart[index].title)
        Espresso.pressBack()
        onComposeScreen<MiniPlayerScreen>(composeRule) { root.assertTextContains(chart[index].title) }
    }

    /** HOME → контроллер UI отвязан (снапшот «не подключён») → сервис остановлен, сессии в системе нет. */
    private fun BaseTestContext.stopPlaybackServiceInBackground() {
        device.uiDevice.pressHome()
        flakySafely(LOAD_TIMEOUT_MS) {
            assertEquals(0, resumedMainActivities().size)
            assertFalse("контроллер ещё подключён", active().isConnected)
        }
        context.stopService(Intent().setComponent(ComponentName(context, PLAYBACK_SERVICE)))
        flakySafely(PLAYBACK_TIMEOUT_MS) { assertNull("сессия жива: ${session().raw}", session().state) }
    }

    private companion object {
        /** Сервис `internal` в `:impl`, `app` его класс не видит. */
        const val PLAYBACK_SERVICE = "com.jayelmeynak.feature.player.impl.service.PlayBackService"
        const val DEEP_LINK = "am start -W -a android.intent.action.VIEW -d multiplayer://main com.jayelmeynak.musicplayer"
    }
}
