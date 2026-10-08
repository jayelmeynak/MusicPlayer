package com.jayelmeynak.feature.player.impl.presentation

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.test.utils.FakeMediaSourceFactory
import androidx.media3.test.utils.TestExoPlayerBuilder
import androidx.media3.test.utils.robolectric.TestPlayerRunHelper.run
import com.jayelmeynak.feature.player.api.TrackSource
import com.jayelmeynak.lib.mediastore.domain.model.LocalTrack
import com.jayelmeynak.lib.mediastore.domain.usecase.GetLocalTracksUseCase
import com.jayelmeynak.lib.mediastore.domain.usecase.GetTrackArtworkUseCase
import com.jayelmeynak.feature.player.impl.domain.models.Track
import com.jayelmeynak.feature.player.impl.domain.usecase.GetLocalTrackListUseCase
import com.jayelmeynak.feature.player.impl.domain.usecase.GetRemoteAlbumUseCase
import com.jayelmeynak.feature.player.impl.domain.usecase.GetRemoteTrackUseCase
import com.jayelmeynak.feature.player.impl.navigation.PlayerRequestHolder
import com.jayelmeynak.feature.player.impl.service.EXTRA_TRACK_DURATION_MS
import com.jayelmeynak.feature.player.impl.service.MusicServiceHandler
import com.jayelmeynak.util.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * AudioViewModel depends on the concrete MusicServiceHandler over ExoPlayer and on android.net.Uri,
 * so it runs on Robolectric with a test player and fake media. The tests never run the player
 * looper, so even after a play command isPlaying stays false and the endless progress loop on Main
 * never starts. Running the player until READY here would start it.
 */
@RunWith(RobolectricTestRunner::class)
class AudioViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val scope = CoroutineScope(SupervisorJob())
    private val remoteRepository = FakeMusicRemoteRepository()
    private val localRepository = FakeLocalTracksRepository()
    private val requests = PlayerRequestHolder()
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
    fun `восстановление очереди Deezer - источник api`() {
        handler.setMediaItemList(listOf(mediaItem(1, "https://example.com/1.mp3")))

        val viewModel = createViewModel()

        assertEquals("api", viewModel.source.value)
    }

    @Test
    fun `восстановление локальной очереди - источник local`() {
        handler.setMediaItemList(listOf(mediaItem(1, LOCAL_URI_1)))

        val viewModel = createViewModel()

        assertEquals("local", viewModel.source.value)
    }

    @Test
    fun `пустая очередь - источник не задан`() {
        val viewModel = createViewModel()

        assertEquals("", viewModel.source.value)
    }

    @Test
    fun `позиция без известной длительности - прогресс 0, время по позиции`() {
        player.setMediaItems(listOf(mediaItem(1, "https://example.com/1.mp3")), 0, 5_000)

        val viewModel = createViewModel()

        assertEquals(0L, viewModel.duration.value)
        assertEquals(0f, viewModel.progress.value)
        assertEquals("00:05", viewModel.progressString.value)
    }

    @Test
    fun `позиция при известной длительности - прогресс в процентах`() {
        handler.setMediaItemList(listOf(mediaItem(1, "https://example.com/1.mp3")))
        run(player).untilState(Player.STATE_READY)
        player.seekTo(2_500)

        val viewModel = createViewModel()

        // FakeMediaSourceFactory items last 10 s.
        assertEquals(10_000L, viewModel.duration.value)
        assertEquals(25f, viewModel.progress.value)
    }

    @Test
    fun `перемотка без известной длительности - позиция не меняется`() {
        player.setMediaItems(listOf(mediaItem(1, "https://example.com/1.mp3")), 0, 5_000)
        val viewModel = createViewModel()

        viewModel.onUiEvents(UIEvents.SeekTo(50f))

        assertEquals(5_000L, player.currentPosition)
    }

    @Test
    fun `переход с локального трека с обложкой на трек Deezer без обложки - обложка сброшена`() {
        val artwork = byteArrayOf(1, 2, 3)
        localRepository.tracks = listOf(localTrack(1, LOCAL_URI_1))
        localRepository.artworks = mapOf(1L to artwork)
        remoteRepository.tracks = mapOf("42" to remoteTrack(42))
        val viewModel = createViewModel()

        viewModel.loadLocalTrack(LOCAL_URI_1)
        assertArrayEquals(artwork, viewModel.trackArtwork.value)

        viewModel.loadRemoteTrack("42")

        assertNull(viewModel.trackArtwork.value)
    }

    @Test
    fun `трек Deezer попадает в очередь плеера с полной длительностью, не как длительность медиа`() {
        remoteRepository.tracks = mapOf("42" to remoteTrack(42).copy(duration = 225_000))
        val viewModel = createViewModel()

        viewModel.loadRemoteTrack("42")

        val metadata = player.getMediaItemAt(0).mediaMetadata
        assertEquals(225_000, metadata.extras?.getInt(EXTRA_TRACK_DURATION_MS))
        assertNull(metadata.durationMs)
    }

    @Test
    fun `перемотка на паузе сразу двигает прогресс и время`() {
        handler.setMediaItemList(listOf(mediaItem(1, "https://example.com/1.mp3")))
        run(player).untilState(Player.STATE_READY)
        val viewModel = createViewModel()

        viewModel.onUiEvents(UIEvents.SeekTo(50f))

        assertEquals(50f, viewModel.progress.value)
        assertEquals("00:05", viewModel.progressString.value)
    }

    @Test
    fun `поздняя обложка прошлого трека не попадает в состояние нового`() {
        val lateArtwork = CompletableDeferred<ByteArray?>()
        localRepository.tracks = listOf(localTrack(1, LOCAL_URI_1), localTrack(2, LOCAL_URI_2))
        localRepository.pendingArtworks[1L] = lateArtwork
        val viewModel = createViewModel()

        viewModel.loadLocalTrack(LOCAL_URI_1)
        viewModel.loadLocalTrack(LOCAL_URI_2)
        lateArtwork.complete(byteArrayOf(1, 2, 3))

        assertEquals(2L, viewModel.currentSelectedAudio.value.id)
        assertNull(viewModel.trackArtwork.value)
    }

    @Test
    fun `открытие плеера с запросом Deezer - трек загружен в очередь`() {
        remoteRepository.tracks = mapOf("42" to remoteTrack(42))
        val viewModel = createViewModel()

        requests.open(TrackSource.DEEZER, "42")
        viewModel.onPlayerOpened()

        assertEquals(42L, viewModel.currentSelectedAudio.value.id)
        assertEquals("api", viewModel.source.value)
        assertEquals("42", player.getMediaItemAt(0).mediaId)
    }

    @Test
    fun `открытие плеера с локальным запросом - выбран трек из MediaStore`() {
        localRepository.tracks = listOf(localTrack(1, LOCAL_URI_1), localTrack(2, LOCAL_URI_2))
        val viewModel = createViewModel()

        requests.open(TrackSource.LOCAL, LOCAL_URI_2)
        viewModel.onPlayerOpened()

        assertEquals(2L, viewModel.currentSelectedAudio.value.id)
        assertEquals("local", viewModel.source.value)
        assertEquals(1, player.currentMediaItemIndex)
    }

    @Test
    fun `открытие плеера без запроса - текущий трек не меняется`() {
        handler.setMediaItemList(listOf(mediaItem(1, "https://example.com/1.mp3")))
        remoteRepository.tracks = mapOf("42" to remoteTrack(42))
        val viewModel = createViewModel()

        viewModel.onPlayerOpened()

        assertEquals(1L, viewModel.currentSelectedAudio.value.id)
        assertEquals(1, player.mediaItemCount)
        assertEquals("1", player.getMediaItemAt(0).mediaId)
    }

    @Test
    fun `повторное открытие плеера не проигрывает прошлый запрос заново`() {
        remoteRepository.tracks = mapOf("42" to remoteTrack(42))
        localRepository.tracks = listOf(localTrack(1, LOCAL_URI_1))
        val viewModel = createViewModel()
        requests.open(TrackSource.DEEZER, "42")
        viewModel.onPlayerOpened()
        viewModel.loadLocalTrack(LOCAL_URI_1)

        viewModel.onPlayerOpened()

        assertEquals(1L, viewModel.currentSelectedAudio.value.id)
        assertEquals("local", viewModel.source.value)
    }

    @Test
    fun `открытие плеера без запроса при пустой очереди - экран закрывается`() {
        val viewModel = createViewModel()

        assertFalse(viewModel.onPlayerOpened())
    }

    @Test
    fun `открытие плеера без запроса при непустой очереди - экран остаётся`() {
        handler.setMediaItemList(listOf(mediaItem(1, "https://example.com/1.mp3")))
        val viewModel = createViewModel()

        assertTrue(viewModel.onPlayerOpened())
    }

    @Test
    fun `открытие плеера с запросом при пустой очереди - экран остаётся`() {
        remoteRepository.tracks = mapOf("42" to remoteTrack(42))
        val viewModel = createViewModel()
        requests.open(TrackSource.DEEZER, "42")

        assertTrue(viewModel.onPlayerOpened())
    }

    private fun createViewModel() = AudioViewModel(
        audioServiceHandler = handler,
        getLocalTrackListUseCase = GetLocalTrackListUseCase(GetLocalTracksUseCase(localRepository)),
        getRemoteTrackUseCase = GetRemoteTrackUseCase(remoteRepository),
        getRemoteAlbumUseCase = GetRemoteAlbumUseCase(remoteRepository),
        getTrackArtworkUseCase = GetTrackArtworkUseCase(localRepository),
        playerRequests = requests,
    )

    private fun mediaItem(id: Long, uri: String): MediaItem = MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(uri)
        .setMediaMetadata(MediaMetadata.Builder().setTitle("Title $id").build())
        .build()

    private fun localTrack(id: Long, uri: String) = LocalTrack(
        id = id,
        title = "Local $id",
        artistName = "Artist $id",
        duration = 180_000,
        uri = Uri.parse(uri),
    )

    private fun remoteTrack(id: Long) = Track(
        id = id,
        title = "Remote $id",
        album = null,
        artistName = "Artist $id",
        preview = "https://example.com/$id.mp3",
        uri = null,
    )

    private companion object {
        const val LOCAL_URI_1 = "content://media/external/audio/media/1"
        const val LOCAL_URI_2 = "content://media/external/audio/media/2"
    }
}
