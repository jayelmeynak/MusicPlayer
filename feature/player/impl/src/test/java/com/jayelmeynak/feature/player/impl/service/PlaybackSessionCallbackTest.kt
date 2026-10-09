package com.jayelmeynak.feature.player.impl.service

import android.net.Uri
import android.os.Bundle
import android.os.Process
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaLibraryInfo
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.ControllerInfo
import androidx.media3.test.utils.TestExoPlayerBuilder
import com.jayelmeynak.feature.player.api.QueueItem
import com.jayelmeynak.feature.player.api.TrackSource
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
 * Robolectric: MediaItem, Uri and MediaSession are Android classes. The callback is called
 * directly: adding items needs no session; connection decisions get a real session over a test
 * player and a [ControllerInfo] built for tests, since a foreign uid cannot connect in-process.
 */
@OptIn(UnstableApi::class)
@RunWith(RobolectricTestRunner::class)
class PlaybackSessionCallbackTest {

    private val callback = PlaybackSessionCallback()
    private var player: ExoPlayer? = null
    private var session: MediaSession? = null

    @After
    fun tearDown() {
        session?.release()
        player?.release()
    }

    @Test
    fun `чужое недоверенное приложение не подключается и не читает очередь`() {
        val result = callback.onConnect(session(), controller(uid = FOREIGN_UID, trusted = false))

        assertFalse(result.isAccepted)
    }

    @Test
    fun `чужой недоверенный платформенный контроллер не подключается`() {
        val result = callback.onConnect(
            session(),
            controller(uid = FOREIGN_UID, trusted = false, version = ControllerInfo.LEGACY_CONTROLLER_VERSION),
        )

        assertFalse(result.isAccepted)
    }

    @Test
    fun `чужое приложение, назвавшееся пакетом приложения, не подключается`() {
        val result = callback.onConnect(
            session(),
            controller(packageName = OWN_PACKAGE, uid = FOREIGN_UID, trusted = false),
        )

        assertFalse(result.isAccepted)
    }

    @Test
    fun `доверенный контроллер Media3 с непроверенным пакетом не подключается`() {
        val result = callback.onConnect(
            session(),
            controller(packageName = "com.android.systemui", uid = FOREIGN_UID, trusted = true, verified = false),
        )

        assertFalse(result.isAccepted)
    }

    @Test
    fun `доверенный платформенный контроллер подключается и без проверки пакета`() {
        val result = callback.onConnect(
            session(),
            controller(
                packageName = "android",
                uid = Process.SYSTEM_UID,
                trusted = true,
                version = ControllerInfo.LEGACY_CONTROLLER_VERSION,
                verified = false,
            ),
        )

        assertTrue(result.isAccepted)
    }

    @Test
    fun `контроллер своего приложения получает полный доступ, включая очередь`() {
        val result = callback.onConnect(
            session(),
            controller(packageName = OWN_PACKAGE, uid = Process.myUid(), trusted = false),
        )

        assertTrue(result.isAccepted)
        assertTrue(result.availablePlayerCommands.contains(Player.COMMAND_GET_TIMELINE))
    }

    @Test
    fun `доверенный системой контроллер получает полный доступ, включая очередь`() {
        val result = callback.onConnect(
            session(),
            controller(packageName = "com.android.systemui", uid = FOREIGN_UID, trusted = true),
        )

        assertTrue(result.isAccepted)
        assertTrue(result.availablePlayerCommands.contains(Player.COMMAND_GET_TIMELINE))
    }

    @Test
    fun `доверенный платформенный контроллер системы - медиакнопки и шторка - получает полный доступ`() {
        val result = callback.onConnect(
            session(),
            controller(
                packageName = "android",
                uid = Process.SYSTEM_UID,
                trusted = true,
                version = ControllerInfo.LEGACY_CONTROLLER_VERSION,
            ),
        )

        assertTrue(result.isAccepted)
        assertTrue(result.availablePlayerCommands.contains(Player.COMMAND_GET_TIMELINE))
    }

    @Test
    fun `элементы очереди получают внутренний URI источника и id, а не URL`() {
        val items = listOf(
            queueItem("42", TrackSource.DEEZER).toMediaItem(),
            queueItem(LOCAL_ID, TrackSource.LOCAL).toMediaItem(),
        )

        val added = callback.prepareMediaItems(items)

        assertEquals(TrackKey(TrackSource.DEEZER, "42"), added[0].localConfiguration?.uri?.toTrackKey())
        assertEquals(TrackKey(TrackSource.LOCAL, LOCAL_ID), added[1].localConfiguration?.uri?.toTrackKey())
        added.forEach { assertEquals(TRACK_URI_SCHEME, it.localConfiguration?.uri?.scheme) }
    }

    @Test
    fun `метаданные и mediaId сохраняются`() {
        val item = queueItem("42", TrackSource.DEEZER)

        val added = callback.prepareMediaItems(listOf(item.toMediaItem())).single()

        assertEquals(item, added.toQueueItem())
    }

    @Test
    fun `чужой элемент со своим URI не играется как есть - получает неразрешимый внутренний URI`() {
        val foreign = listOf(
            "file:///sdcard/Music/1.mp3",
            "content://com.example.provider/secret",
            "content://media/external/audio/media/7",
            "https://example.com/1.mp3",
        ).map(MediaItem::fromUri)

        val added = callback.prepareMediaItems(foreign)

        added.forEach { item ->
            val uri = item.localConfiguration?.uri
            assertEquals(TRACK_URI_SCHEME, uri?.scheme)
            assertNull(uri?.toTrackKey())
        }
    }

    @Test
    fun `чужой элемент без URI получает внутренний URI, который не разрешится - ошибка плеера`() {
        val foreign = MediaItem.Builder()
            .setMediaId("42")
            .setMediaMetadata(MediaMetadata.Builder().setTitle("Foreign").build())
            .build()

        val added = callback.prepareMediaItems(listOf(foreign)).single()

        assertEquals(TRACK_URI_SCHEME, added.localConfiguration?.uri?.scheme)
        assertNull(added.localConfiguration?.uri?.toTrackKey())
        assertEquals(Uri.parse(added.localConfiguration?.uri.toString()), added.localConfiguration?.uri)
    }

    private fun session(): MediaSession {
        val context = RuntimeEnvironment.getApplication()
        val testPlayer = TestExoPlayerBuilder(context).build().also { player = it }
        return MediaSession.Builder(context, testPlayer).build().also { session = it }
    }

    private fun controller(
        packageName: String = "com.example.foreign",
        uid: Int,
        trusted: Boolean,
        version: Int = MediaLibraryInfo.VERSION_INT,
        verified: Boolean = true,
    ): ControllerInfo = ControllerInfo.createTestOnlyControllerInfo(
        packageName,
        /* pid = */ 0,
        uid,
        version,
        /* interfaceVersion = */ if (version == ControllerInfo.LEGACY_CONTROLLER_VERSION) 0 else MediaLibraryInfo.INTERFACE_VERSION,
        trusted,
        Bundle.EMPTY,
        /* isPackageNameVerified = */ verified,
    )

    private fun queueItem(id: String, source: TrackSource) = QueueItem(
        id = id,
        source = source,
        title = "Title $id",
        artist = "Artist",
        artworkUri = null,
        durationMs = 1_000L,
    )

    private companion object {
        const val LOCAL_ID = "content://media/external/audio/media/7"
        const val OWN_PACKAGE = "com.jayelmeynak.feature.player.impl.test"
        const val FOREIGN_UID = 10_999
    }
}
