package com.jayelmeynak.feature.player.impl.playback

import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.test.utils.FakeMediaSourceFactory
import androidx.media3.test.utils.TestExoPlayerBuilder
import androidx.media3.test.utils.robolectric.TestPlayerRunHelper.run
import com.jayelmeynak.feature.player.api.PlaybackError
import com.jayelmeynak.feature.player.api.QueueItem
import com.jayelmeynak.feature.player.api.TrackSource
import com.jayelmeynak.feature.player.api.testing.FakeTrackUriResolver
import com.jayelmeynak.feature.player.impl.service.PlaybackSessionCallback
import com.jayelmeynak.feature.player.impl.service.TrackUriDataSpecResolver
import com.jayelmeynak.feature.player.impl.service.toMediaItem
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Robolectric runs a test ExoPlayer: media from [FakeMediaSourceFactory] (or the real data source
 * stack for the error case), time from the auto-advancing fake clock.
 */
@RunWith(RobolectricTestRunner::class)
class PlaybackStateMappingTest {

    private val context = RuntimeEnvironment.getApplication()
    private val players = mutableListOf<ExoPlayer>()

    @After
    fun tearDown() {
        players.forEach { it.release() }
    }

    @Test
    fun `снапшот несёт очередь, индекс и флаги плеера, неизвестная длительность - 0`() {
        val player = fakePlayer()
        player.setMediaItems(sessionItems(), 1, 0L)

        val state = player.toPlaybackState()

        assertEquals(queue, state.queue)
        assertEquals(1, state.currentIndex)
        assertEquals(queue[1], state.current)
        assertFalse(state.isPlaying)
        assertEquals(0L, state.durationMs)
        assertNull(state.error)
    }

    @Test
    fun `после подготовки - длительность текущего элемента`() {
        val player = fakePlayer()
        player.setMediaItems(sessionItems())
        player.prepare()
        run(player).untilState(Player.STATE_READY)

        assertEquals(player.duration, player.toPlaybackState().durationMs)
        assertTrue(player.toPlaybackState().durationMs > 0)
    }

    @Test
    fun `next меняет индекс, isPlaying и индекс согласованы в одном снапшоте`() {
        val player = fakePlayer()
        player.setMediaItems(sessionItems())
        player.prepare()
        player.play()
        run(player).untilState(Player.STATE_READY)

        player.seekToNext()
        val state = player.toPlaybackState()

        assertEquals(1, state.currentIndex)
        assertEquals(player.isPlaying, state.isPlaying)
        assertTrue(state.playWhenReady)
    }

    @Test
    fun `автопереход в конце трека меняет индекс в снапшоте`() {
        val player = fakePlayer()
        player.setMediaItems(sessionItems())
        player.prepare()
        player.play()

        run(player).untilPositionDiscontinuityWithReason(Player.DISCONTINUITY_REASON_AUTO_TRANSITION)

        assertEquals(1, player.toPlaybackState().currentIndex)
        assertEquals(queue[1], player.toPlaybackState().current)
    }

    @Test
    fun `резолвер не дал URI - ошибка источника в снапшоте`() {
        val resolver = TrackUriDataSpecResolver(mapOf(TrackSource.DEEZER to FakeTrackUriResolver()))
        val player = TestExoPlayerBuilder(context)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(
                    ResolvingDataSource.Factory(DefaultDataSource.Factory(context), resolver),
                )
            )
            .build()
            .also { players += it }
        player.setMediaItems(sessionItems())
        player.prepare()
        player.play()

        run(player).untilPlayerError()

        val state = player.toPlaybackState()
        assertEquals(PlaybackError.SOURCE_UNAVAILABLE, state.error)
        // Воспроизведение всё ещё запрошено, но тап повторит попытку: кнопка должна предлагать play.
        assertTrue(state.playWhenReady)
        assertTrue(state.showPlayButton)
    }

    @Test
    fun `буферизация при запрошенном воспроизведении - кнопка паузы`() {
        val player = fakePlayer()
        player.setMediaItems(sessionItems())
        player.prepare()
        player.play()

        val state = player.toPlaybackState()

        assertTrue(state.isBuffering)
        assertFalse(state.isPlaying)
        assertFalse(state.showPlayButton)
    }

    @Test
    fun `пауза и конец очереди - кнопка воспроизведения`() {
        val player = fakePlayer()
        player.setMediaItems(sessionItems(), 2, 0L)
        player.prepare()
        player.play()
        run(player).untilState(Player.STATE_READY)
        assertFalse(player.toPlaybackState().showPlayButton)

        player.pause()
        assertTrue(player.toPlaybackState().showPlayButton)

        player.play()
        run(player).untilState(Player.STATE_ENDED)
        val ended = player.toPlaybackState()
        assertTrue(ended.playWhenReady)
        assertTrue(ended.showPlayButton)
    }

    @Test
    fun `ошибки ввода-вывода - недоступный источник, остальные - неизвестная ошибка`() {
        val io = PlaybackException("io", null, PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED)
        val decoder = PlaybackException("decoder", null, PlaybackException.ERROR_CODE_DECODING_FAILED)

        val file = PlaybackException("file", null, PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND)
        val parsing = PlaybackException("parsing", null, PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED)

        assertEquals(PlaybackError.SOURCE_UNAVAILABLE, io.toPlaybackError())
        assertEquals(PlaybackError.SOURCE_UNAVAILABLE, file.toPlaybackError())
        assertEquals(PlaybackError.UNKNOWN, decoder.toPlaybackError())
        assertEquals(PlaybackError.UNKNOWN, parsing.toPlaybackError())
    }

    @Test
    fun `пустой плеер - пустая очередь`() {
        val state = fakePlayer().toPlaybackState()

        assertTrue(state.queue.isEmpty())
        assertNull(state.current)
    }

    private fun fakePlayer(): ExoPlayer = TestExoPlayerBuilder(context)
        .setMediaSourceFactory(FakeMediaSourceFactory())
        .build()
        .also { players += it }

    private fun sessionItems() = PlaybackSessionCallback().prepareMediaItems(queue.map { it.toMediaItem() })

    private val queue = listOf(
        QueueItem("1", TrackSource.DEEZER, "One", "A", "https://example.com/1.jpg", 200_000L),
        QueueItem("2", TrackSource.DEEZER, "Two", "B", null, 0L),
        QueueItem("content://media/external/audio/media/3", TrackSource.LOCAL, "Three", "C", null, 3_000L),
    )
}
