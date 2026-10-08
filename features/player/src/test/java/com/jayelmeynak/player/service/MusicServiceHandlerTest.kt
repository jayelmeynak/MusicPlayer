package com.jayelmeynak.player.service

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.test.utils.FakeMediaSourceFactory
import androidx.media3.test.utils.TestExoPlayerBuilder
import androidx.media3.test.utils.robolectric.TestPlayerRunHelper.run
import app.cash.turbine.Event
import app.cash.turbine.test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Characterization tests: they pin the current behavior, known defects included — the current
 * track index is lost on track change without a play/pause flip, and back-to-back state writes
 * overwrite each other in the StateFlow.
 * Media comes from [FakeMediaSourceFactory], time from the auto-advancing fake clock.
 */
@RunWith(RobolectricTestRunner::class)
class MusicServiceHandlerTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val scope = CoroutineScope(SupervisorJob())
    private lateinit var player: ExoPlayer
    private lateinit var handler: MusicServiceHandler

    @Before
    fun setUp() {
        player = TestExoPlayerBuilder(RuntimeEnvironment.getApplication())
            .setMediaSourceFactory(FakeMediaSourceFactory())
            .build()
        handler = MusicServiceHandler(player, scope)
    }

    @After
    fun tearDown() {
        handler.release()
        player.release()
        scope.cancel()
    }

    @Test
    fun `до подготовки - Initial, duration 0 вместо TIME_UNSET`() {
        assertEquals(MusicState.Initial, handler.audioState.value)
        assertEquals(0L, handler.duration())
    }

    @Test
    fun `после подготовки - Ready с длительностью трека`() {
        handler.setMediaItemList(mediaItems(3))

        run(player).untilState(Player.STATE_READY)

        assertEquals(MusicState.Ready(player.duration), handler.audioState.value)
    }

    @Test
    fun `выбор другого трека - сразу Playing(true), плеер переходит на трек`() {
        handler.setMediaItemList(mediaItems(3))
        run(player).untilState(Player.STATE_READY)

        handler.onPlayerEvents(PlayerEvent.SelectedAudioChange, selectedAudioIndex = 1)

        assertEquals(MusicState.Playing(isPlaying = true), handler.audioState.value)
        assertTrue(player.playWhenReady)
        assertEquals(1, handler.currentMediaItemIndex())
    }

    @Test
    fun `старт воспроизведения - последнее состояние Playing(true)`() {
        handler.setMediaItemList(mediaItems(3))
        handler.onPlayerEvents(PlayerEvent.SelectedAudioChange, selectedAudioIndex = 1)

        awaitPlaying()

        assertTrue(handler.isCurrentlyPlaying())
        assertEquals(MusicState.Playing(isPlaying = true), handler.audioState.value)
    }

    @Test
    fun `дефект - при старте подписчик не видит CurrentPlaying, его сразу перетирает Playing`() = runTest(ownScheduler()) {
        handler.setMediaItemList(mediaItems(3))
        run(player).untilState(Player.STATE_READY)
        // A UI collector is dispatched, not resumed inline: it reads the StateFlow after both writes.
        val seen = mutableListOf<MusicState>()
        val collector = launch(StandardTestDispatcher(testScheduler)) {
            handler.audioState.collect { seen += it }
        }
        runCurrent()

        player.play()
        awaitPlaying()
        runCurrent()

        assertFalse(seen.any { it is MusicState.CurrentPlaying })
        assertEquals(MusicState.Playing(isPlaying = true), seen.last())
        collector.cancel()
    }

    @Test
    fun `дефект - переход на следующий трек без паузы и старта не публикует новый индекс трека`() =
        runTest(ownScheduler()) {
            // Gapless auto-transition is not reproducible here: fake media rebuffers between items,
            // isPlaying flips and CurrentPlaying arrives. A paused SeekToNext keeps isPlaying stable.
            handler.setMediaItemList(mediaItems(3))
            run(player).untilState(Player.STATE_READY)

            handler.audioState.test {
                assertEquals(MusicState.Ready(player.duration), awaitItem())

                handler.onPlayerEvents(PlayerEvent.SeekToNext)
                run(player).untilState(Player.STATE_READY)

                assertEquals(1, player.currentMediaItemIndex)
                val events = cancelAndConsumeRemainingEvents()
                assertFalse(events.any { it is Event.Item && it.value is MusicState.CurrentPlaying })
            }
        }

    @Test
    fun `PlayPause во время воспроизведения - пауза и Playing(false)`() {
        handler.setMediaItemList(mediaItems(3))
        player.play()
        awaitPlaying()

        handler.onPlayerEvents(PlayerEvent.PlayPause)
        run(player).untilPendingCommandsAreFullyHandled()

        assertFalse(player.playWhenReady)
        assertEquals(MusicState.Playing(isPlaying = false), handler.audioState.value)
    }

    @Test
    fun `PlayPause на паузе - запускает воспроизведение`() {
        handler.setMediaItemList(mediaItems(3))
        run(player).untilState(Player.STATE_READY)

        handler.onPlayerEvents(PlayerEvent.PlayPause)

        assertTrue(player.playWhenReady)
    }

    @Test
    fun `SelectedAudioChange на текущий трек работает как PlayPause`() {
        handler.setMediaItemList(mediaItems(3))
        run(player).untilState(Player.STATE_READY)

        handler.onPlayerEvents(PlayerEvent.SelectedAudioChange, selectedAudioIndex = 0)

        assertTrue(player.playWhenReady)
        assertEquals(0, handler.currentMediaItemIndex())
    }

    @Test
    fun `SeekToNext и SeekToPrevious переключают трек в плеере`() {
        handler.setMediaItemList(mediaItems(3))
        run(player).untilState(Player.STATE_READY)

        handler.onPlayerEvents(PlayerEvent.SeekToNext)
        assertEquals(1, player.currentMediaItemIndex)

        handler.onPlayerEvents(PlayerEvent.SeekToPrevious)
        assertEquals(0, player.currentMediaItemIndex)
    }

    @Test
    fun `SeekTo и UpdateProgress перематывают позицию`() {
        handler.setMediaItemList(mediaItems(3))
        run(player).untilState(Player.STATE_READY)

        handler.onPlayerEvents(PlayerEvent.SeekTo, seekPosition = 1_000)
        assertEquals(1_000, player.currentPosition)

        handler.onPlayerEvents(PlayerEvent.UpdateProgress(0.5f))
        assertEquals((player.duration * 0.5f).toLong(), player.currentPosition)
    }

    @Test
    fun `во время воспроизведения каждые 500 мс публикуется Progress`() {
        handler.setMediaItemList(mediaItems(3))
        player.play()
        awaitPlaying()

        mainRule.dispatcher.scheduler.advanceTimeBy(501)

        assertEquals(MusicState.Progress(player.currentPosition), handler.audioState.value)
    }

    @Test
    fun `restorePlaylist собирает треки из MediaItem плеера`() {
        handler.setMediaItemList(mediaItems(2) + MediaItem.Builder().setMediaId("not-a-number").build())

        val tracks = handler.restorePlaylist()

        assertEquals(3, tracks.size)
        with(tracks[1]) {
            assertEquals(11L, id)
            assertEquals("Title 1", title)
            assertEquals("Artist 1", artistName)
            assertEquals("https://example.com/1.mp3", preview)
            assertEquals(Uri.parse("https://example.com/1.mp3"), uri)
            assertEquals("https://example.com/1.jpg", album?.cover)
        }
        with(tracks[2]) {
            assertEquals(2L, id)
            assertEquals("", title)
            assertEquals("", preview)
            assertEquals(null, uri)
            assertEquals("", album?.cover)
        }
    }

    // The progress loop runs on Dispatchers.Main forever; with a shared scheduler runTest would spin it.
    // StandardTestDispatcher() without an argument takes the scheduler of the test Main dispatcher.
    private fun ownScheduler() = StandardTestDispatcher(TestCoroutineScheduler())

    private fun awaitPlaying() {
        run(player).untilState(Player.STATE_READY)
        run(player).untilPendingCommandsAreFullyHandled()
        check(player.isPlaying)
    }

    private fun mediaItems(count: Int): List<MediaItem> = (0 until count).map { i ->
        MediaItem.Builder()
            .setMediaId("${10 + i}")
            .setUri("https://example.com/$i.mp3")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("Title $i")
                    .setArtist("Artist $i")
                    .setArtworkUri(Uri.parse("https://example.com/$i.jpg"))
                    .build()
            )
            .build()
    }
}
