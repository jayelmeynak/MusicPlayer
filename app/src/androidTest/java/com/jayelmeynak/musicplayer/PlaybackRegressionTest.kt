package com.jayelmeynak.musicplayer

import androidx.test.espresso.Espresso
import com.jayelmeynak.feature.player.api.key
import com.jayelmeynak.musicplayer.screen.MiniPlayerScreen
import com.jayelmeynak.musicplayer.screen.PlayerScreen
import com.jayelmeynak.musicplayer.screen.TrackListScreen
import com.jayelmeynak.musicplayer.testing.MusicPlayerTestCase
import com.jayelmeynak.musicplayer.testing.SessionPlayerEvents
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Регресс воспроизведения с экрана плеера и списка на настоящем сервисе Media3. */
@HiltAndroidTest
class PlaybackRegressionTest : MusicPlayerTestCase() {

    @Test
    fun `тап_по_треку__трек_играет_позиция_растёт_есть_сессия_и_уведомление`() = run {
        var startPositionMs = 0L
        step("Тап по первому треку чарта") {
            playFromChart(0)
            startPositionMs = controller().positionMs.value
        }
        step("Экран плеера показывает трек и кнопку паузы") {
            onComposeScreen<PlayerScreen>(composeRule) {
                title(chart[0].title).assertIsDisplayed()
                pauseButton.assertIsDisplayed()
            }
        }
        step("Позиция растёт от стартовой") {
            flakySafely(PLAYBACK_TIMEOUT_MS) {
                assertTrue(controller().positionMs.value >= startPositionMs + 1_500)
            }
        }
        step("Системная сессия играет этот трек") { awaitSession(STATE_PLAYING, chart[0].title) }
        step("Уведомление плеера с заголовком трека") { awaitNotificationTitle(chart[0].title) }
    }

    @Test
    fun `пауза_и_продолжение_кнопкой_экрана_плеера_меняют_UI_и_сессию`() = run {
        step("Трек играет") { playFromChart(0) }
        step("Пауза") {
            onComposeScreen<PlayerScreen>(composeRule) { pauseButton.performClick() }
            awaitPaused(chart[0].title)
            onComposeScreen<PlayerScreen>(composeRule) { playButton.assertIsDisplayed() }
            awaitSession(STATE_PAUSED, chart[0].title)
        }
        step("Продолжение") {
            onComposeScreen<PlayerScreen>(composeRule) { playButton.performClick() }
            awaitPlaying(chart[0].title)
            onComposeScreen<PlayerScreen>(composeRule) { pauseButton.assertIsDisplayed() }
            awaitSession(STATE_PLAYING, chart[0].title)
        }
    }

    @Test
    fun `next_с_экрана_плеера_меняет_трек_на_экране_в_мини-плеере_и_в_сессии`() = run {
        step("Трек 1 играет") { playFromChart(0) }
        step("Следующий трек") {
            onComposeScreen<PlayerScreen>(composeRule) { nextButton.performClick() }
            awaitPlaying(chart[1].title)
            onComposeScreen<PlayerScreen>(composeRule) { title(chart[1].title).assertIsDisplayed() }
        }
        step("Сессия и уведомление — трек 2") {
            awaitSession(STATE_PLAYING, chart[1].title)
            awaitNotificationTitle(chart[1].title)
        }
        step("Мини-плеер на списке — трек 2") {
            Espresso.pressBack()
            onComposeScreen<MiniPlayerScreen>(composeRule) { root.assertTextContains(chart[1].title) }
        }
    }

    @Test
    fun `prev_сразу_после_next_возвращает_предыдущий_трек_видимого_списка`() = run {
        step("Трек 2 играет") { playFromChart(1) }
        step("Следующий, затем предыдущий") {
            onComposeScreen<PlayerScreen>(composeRule) { nextButton.performClick() }
            awaitPlaying(chart[2].title)
            onComposeScreen<PlayerScreen>(composeRule) { previousButton.performClick() }
            awaitPlaying(chart[1].title)
            assertEquals(chart.map { it.id.toString() }, active().queue.map { it.id })
        }
    }

    @Test
    fun `короткий_трек_заканчивается__автопереход_на_следующий`() = run {
        step("Короткий трек 4 играет") { playFromChart(3) }
        step("Трек 5 стал текущим и играет") {
            flakySafely(AUTO_ADVANCE_TIMEOUT_MS) {
                assertEquals(chart[4].title, active().current?.title)
                assertTrue(active().isPlaying)
            }
            onComposeScreen<PlayerScreen>(composeRule) { title(chart[4].title).assertIsDisplayed() }
            awaitSession(STATE_PLAYING, chart[4].title)
        }
    }

    @Test
    fun `повторный_тап_по_играющему_треку_не_перезапускает_его`() = run {
        step("Трек играет") { playFromChart(0) }
        lateinit var events: SessionPlayerEvents
        step("Контроллер теста подключён к сессии") { events = SessionPlayerEvents.connect(context, LOAD_TIMEOUT_MS) }
        events.use { session ->
            val mediaId = checkNotNull(active().current).key
            step("Снова тап по тому же треку") {
                assertEquals(mediaId, session.currentMediaId())
                Espresso.pressBack()
                onComposeScreen<TrackListScreen>(composeRule) { track(chart[0].title).performClick() }
                onComposeScreen<PlayerScreen>(composeRule) { title(chart[0].title).assertIsDisplayed() }
            }
            step("Плеер сессии не перезапускал трек: тот же элемент, играет") {
                // Сервис в том же процессе: команды play Media3 маскирует синхронно, события сессии
                // приходят сообщениями главного looper — onIdle разбирает его очередь до пустой.
                Espresso.onIdle()
                assertEquals(emptyList<String>(), session.restarts())
                assertEquals(mediaId, session.currentMediaId())
                assertEquals(0, active().currentIndex)
                awaitPlaying(chart[0].title)
            }
        }
    }

    private companion object {
        const val AUTO_ADVANCE_TIMEOUT_MS = 20_000L
    }
}
