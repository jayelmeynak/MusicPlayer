package com.jayelmeynak.musicplayer.presentation

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.SemanticsMatcher
import com.jayelmeynak.feature.player.api.testing.FakePlaybackController
import com.jayelmeynak.feature.player.api.testing.FakePlaybackController.Command
import com.jayelmeynak.musicplayer.screen.MiniPlayerScreen
import com.jayelmeynak.musicplayer.screen.NavigationBarScreen
import com.jayelmeynak.musicplayer.screen.TrackListScreen
import com.jayelmeynak.musicplayer.testing.FakeDeezerServer
import com.jayelmeynak.musicplayer.testing.FakeDeezerServerRule
import com.jayelmeynak.musicplayer.testing.TestPlayerModule.PLAYER_STUB_TAG
import com.jayelmeynak.musicplayer.testing.TestPlayerModule.PLAYER_STUB_TEXT
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.inject.Inject

/**
 * Оболочка приложения целиком (`MainActivity`, вкладки, стеки, мини-плеер) с фейковым Deezer и
 * фейковым контроллером плеера. Robolectric — чтобы гонять без устройства в обычном CI job.
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class AppShellTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val serverRule = FakeDeezerServerRule()

    @get:Rule(order = 2)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var controller: FakePlaybackController

    private val chart = FakeDeezerServer.chartTracks

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun `тап по треку чарта открывает плеер и запускает весь видимый список с индексом трека`() {
        openPlayerFromChart(index = 1)

        val play = controller.commands.last() as Command.Play
        assertEquals(chart.map { it.id.toString() }, play.queue.map { it.id })
        assertEquals(1, play.startIndex)
        onComposeScreen<MiniPlayerScreen>(composeRule) { root { assertDoesNotExist() } }
    }

    @Test
    fun `тап по треку выдачи поиска запускает выдачу поиска`() {
        waitForTrack(chart.first().title)
        onComposeScreen<TrackListScreen>(composeRule) { searchField { performTextInput("тест") } }
        val found = FakeDeezerServer.searchTracks
        waitForTrack(found[1].title)

        onComposeScreen<TrackListScreen>(composeRule) { track(found[1].title).performClick() }

        waitForPlayerStub(found[1].title)
        val play = controller.commands.last() as Command.Play
        assertEquals(found.map { it.id.toString() }, play.queue.map { it.id })
        assertEquals(1, play.startIndex)
    }

    @Test
    fun `плеер в Remote, переход на Local и back - плеер в Remote на месте, чарт не перезагружен`() {
        openPlayerFromChart(index = 0)

        onComposeScreen<NavigationBarScreen>(composeRule) { localTab { performClick() } }
        assertPlayerStubAbsent()
        back()

        onComposeScreen<NavigationBarScreen>(composeRule) { remoteTab { assertIsSelected() } }
        waitForPlayerStub(chart[0].title)
        back()
        waitForTrack(chart[0].title)
        assertPlayerStubAbsent()
        assertEquals(1, FakeDeezerServer.requestCount("/chart"))
    }

    @Test
    fun `back с корня Local открывает Remote`() {
        waitForTrack(chart.first().title)
        onComposeScreen<NavigationBarScreen>(composeRule) {
            localTab { performClick() }
            localTab { assertIsSelected() }
        }

        back()

        onComposeScreen<NavigationBarScreen>(composeRule) { remoteTab { assertIsSelected() } }
        waitForTrack(chart.first().title)
    }

    @Test
    fun `back с корня Remote закрывает приложение`() {
        waitForTrack(chart.first().title)

        back()

        assertTrue(composeRule.activity.isFinishing)
    }

    @Test
    fun `тап по активной вкладке сбрасывает её стек до списка`() {
        openPlayerFromChart(index = 0)

        onComposeScreen<NavigationBarScreen>(composeRule) { remoteTab { performClick() } }

        assertPlayerStubAbsent()
        waitForTrack(chart[0].title)
    }

    @Test
    fun `мини-плеер скрыт до воспроизведения, на списке показывает трек и открывает плеер`() {
        waitForTrack(chart.first().title)
        onComposeScreen<MiniPlayerScreen>(composeRule) { root { assertDoesNotExist() } }
        openPlayerFromChart(index = 2)
        back()

        onComposeScreen<MiniPlayerScreen>(composeRule) {
            root {
                assertIsDisplayed()
                assertTextContains(chart[2].title)
                performClick()
            }
        }

        waitForPlayerStub(chart[2].title)
        onComposeScreen<MiniPlayerScreen>(composeRule) { root { assertDoesNotExist() } }
    }

    @Test
    fun `клик по мини-плееру на Local переносит плеер в Local без дубля в Remote`() {
        openPlayerFromChart(index = 0)
        onComposeScreen<NavigationBarScreen>(composeRule) { localTab { performClick() } }

        onComposeScreen<MiniPlayerScreen>(composeRule) { root { performClick() } }

        waitForPlayerStub(chart[0].title)
        onComposeScreen<NavigationBarScreen>(composeRule) { localTab { assertIsSelected() } }
        back()
        assertPlayerStubAbsent()
        onComposeScreen<NavigationBarScreen>(composeRule) { localTab { assertIsSelected() } }
        back()
        // Корень Remote: плеер перенесён в Local, в Remote его нет.
        onComposeScreen<NavigationBarScreen>(composeRule) { remoteTab { assertIsSelected() } }
        waitForTrack(chart[0].title)
        assertPlayerStubAbsent()
    }

    @Test
    fun `пересоздание с плеером в Local - Local выбрана, плеер виден, мини-плеера нет`() {
        openPlayerFromChart(index = 0)
        onComposeScreen<NavigationBarScreen>(composeRule) { localTab { performClick() } }
        onComposeScreen<MiniPlayerScreen>(composeRule) { root { performClick() } }
        waitForPlayerStub(chart[0].title)

        composeRule.activityRule.scenario.recreate()

        waitForPlayerStub(chart[0].title)
        onComposeScreen<NavigationBarScreen>(composeRule) { localTab { assertIsSelected() } }
        onComposeScreen<MiniPlayerScreen>(composeRule) { root { assertDoesNotExist() } }
    }

    private fun openPlayerFromChart(index: Int) {
        waitForTrack(chart[index].title)
        onComposeScreen<TrackListScreen>(composeRule) { track(chart[index].title).performClick() }
        waitForPlayerStub(chart[index].title)
    }

    private fun waitForTrack(title: String) = waitFor(hasText(title))

    private fun waitForPlayerStub(title: String) =
        waitFor(hasTestTag(PLAYER_STUB_TAG) and hasText(PLAYER_STUB_TEXT + title))

    private fun assertPlayerStubAbsent() {
        composeRule.waitForIdle()
        assertTrue(composeRule.onAllNodes(hasTestTag(PLAYER_STUB_TAG)).fetchSemanticsNodes().isEmpty())
    }

    private fun waitFor(matcher: SemanticsMatcher) {
        composeRule.waitUntil(WAIT_MS) { composeRule.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun back() {
        composeRule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()
    }

    private companion object {
        const val WAIT_MS = 5_000L
    }
}
