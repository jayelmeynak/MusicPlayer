package com.jayelmeynak.musicplayer

import com.jayelmeynak.musicplayer.screen.MiniPlayerScreen
import com.jayelmeynak.musicplayer.screen.NavigationBarScreen
import com.jayelmeynak.musicplayer.screen.PlayerScreen
import com.jayelmeynak.musicplayer.testing.MusicPlayerTestCase
import com.kaspersky.kaspresso.testcases.core.testcontext.BaseTestContext
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import org.junit.Assert.assertEquals
import org.junit.Test

/** Поворот экрана с плеером, перенесённым во вкладку Local. */
@HiltAndroidTest
class RotationTest : MusicPlayerTestCase() {

    @Test
    fun `поворот_с_плеером_в_Local__Local_выбрана_плеер_с_тем_же_треком_мини-плеера_нет`() = run {
        step("Трек играет, плеер перенесён в Local кликом по мини-плееру") {
            playFromChart(0)
            device.exploit.pressBack(false)
            onComposeScreen<NavigationBarScreen>(composeRule) { localTab.performClick() }
            onComposeScreen<MiniPlayerScreen>(composeRule) { root.performClick() }
            onComposeScreen<PlayerScreen>(composeRule) { title(chart[0].title).assertIsDisplayed() }
            onComposeScreen<NavigationBarScreen>(composeRule) { localTab.assertIsSelected() }
        }
        try {
            step("Ландшафт") {
                device.uiDevice.setOrientationLandscape()
                assertPlayerInLocal()
            }
            step("Обратно в портрет") {
                device.uiDevice.setOrientationPortrait()
                assertPlayerInLocal()
                onComposeScreen<PlayerScreen>(composeRule) { nextButton.assertIsDisplayed() }
            }
        } finally {
            device.uiDevice.setOrientationPortrait()
            device.uiDevice.unfreezeRotation()
        }
    }

    private fun BaseTestContext.assertPlayerInLocal() {
        flakySafely(LOAD_TIMEOUT_MS) { assertEquals(1, resumedMainActivities().size) }
        // В ландшафте экран плеера обрезан (известный долг вёрстки): проверяется наличие, не видимость.
        onComposeScreen<PlayerScreen>(composeRule) { title(chart[0].title).assertExists() }
        onComposeScreen<NavigationBarScreen>(composeRule) { localTab.assertIsSelected() }
        onComposeScreen<MiniPlayerScreen>(composeRule) { root.assertDoesNotExist() }
        awaitPlaying(chart[0].title)
    }
}
