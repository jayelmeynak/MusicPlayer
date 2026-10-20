package com.jayelmeynak.feature.player.impl.service

import com.jayelmeynak.feature.player.impl.persistence.PlaybackResumption
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowLog
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineExceptionHandler
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
import com.jayelmeynak.feature.player.api.key
import com.jayelmeynak.feature.player.impl.persistence.FakePlaybackStateStore
import com.jayelmeynak.feature.player.impl.persistence.PlaybackStateStore
import com.jayelmeynak.feature.player.impl.persistence.SavedPosition
import com.jayelmeynak.feature.player.impl.persistence.SavedQueue
import com.jayelmeynak.feature.player.impl.persistence.toSaved
import androidx.media3.test.utils.robolectric.RobolectricUtil.runMainLooperUntil
import java.util.concurrent.ExecutionException
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
 * Robolectric: MediaItem, Uri и MediaSession — Android-классы. Колбэк вызывается напрямую:
 * добавлению элементов сессия не нужна; решения о подключении получают настоящую сессию над
 * тестовым плеером и [ControllerInfo], собранный для тестов, — чужой uid не подключить внутри процесса.
 */
@OptIn(UnstableApi::class)
@RunWith(RobolectricTestRunner::class)
class PlaybackSessionCallbackTest {

    private val callback = testSessionCallback()
    private var player: ExoPlayer? = null
    private var session: MediaSession? = null

    @After
    fun tearDown() {
        session?.release()
        player?.release()
    }

    @Test
    fun `чужое недоверенное приложение не подключается и не читает очередь`() {
        val result = connect(session(), controller(uid = FOREIGN_UID, trusted = false))

        assertFalse(result.isAccepted)
    }

    @Test
    fun `чужой недоверенный платформенный контроллер не подключается`() {
        val result = connect(
            session(),
            controller(uid = FOREIGN_UID, trusted = false, version = ControllerInfo.LEGACY_CONTROLLER_VERSION),
        )

        assertFalse(result.isAccepted)
    }

    @Test
    fun `чужое приложение, назвавшееся пакетом приложения, не подключается`() {
        val result = connect(
            session(),
            controller(packageName = OWN_PACKAGE, uid = FOREIGN_UID, trusted = false),
        )

        assertFalse(result.isAccepted)
    }

    @Test
    fun `доверенный контроллер Media3 с непроверенным пакетом не подключается`() {
        val result = connect(
            session(),
            controller(packageName = "com.android.systemui", uid = FOREIGN_UID, trusted = true, verified = false),
        )

        assertFalse(result.isAccepted)
    }

    @Test
    fun `доверенный платформенный контроллер подключается и без проверки пакета`() {
        val result = connect(
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
        val result = connect(
            session(),
            controller(packageName = OWN_PACKAGE, uid = Process.myUid(), trusted = false),
        )

        assertFullAccess(result)
    }

    @Test
    fun `доверенный системой контроллер получает полный доступ, включая очередь`() {
        val result = connect(
            session(),
            controller(packageName = "com.android.systemui", uid = FOREIGN_UID, trusted = true),
        )

        assertFullAccess(result)
    }

    @Test
    fun `доверенный платформенный контроллер системы - медиакнопки и шторка - получает полный доступ`() {
        val result = connect(
            session(),
            controller(
                packageName = "android",
                uid = Process.SYSTEM_UID,
                trusted = true,
                version = ControllerInfo.LEGACY_CONTROLLER_VERSION,
            ),
        )

        assertFullAccess(result)
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

    @Test
    fun `возобновление для воспроизведения - сохранённая очередь, индекс и позиция`() {
        val callback = testSessionCallback(savedStore())

        val future = callback.onPlaybackResumption(session(), ownController(), /* isForPlayback = */ true)
        runMainLooperUntil { future.isDone }

        val result = future.get()
        assertEquals(savedQueue, result.mediaItems.map { it.toQueueItem() })
        assertTrue(result.mediaItems.all { it.localConfiguration?.uri?.toTrackKey() != null })
        assertEquals(1, result.startIndex)
        assertEquals(42_000L, result.startPositionMs)
    }

    @Test
    fun `возобновление только для метаданных - один текущий элемент`() {
        val callback = testSessionCallback(savedStore())

        val future = callback.onPlaybackResumption(session(), ownController(), /* isForPlayback = */ false)
        runMainLooperUntil { future.isDone }

        assertEquals(listOf(savedQueue[1]), future.get().mediaItems.map { it.toQueueItem() })
    }

    @Test
    fun `возобновлять нечего - future с ошибкой`() {
        val callback = testSessionCallback(FakePlaybackStateStore())

        val future = callback.onPlaybackResumption(session(), ownController(), /* isForPlayback = */ true)
        runMainLooperUntil { future.isDone }

        val error = runCatching { future.get() }.exceptionOrNull()
        assertTrue(error is ExecutionException && error.cause is IllegalStateException)
    }

    @Test
    fun `сбой чтения сохранённой очереди - future с ошибкой, процесс не падает`() {
        val broken = object : PlaybackStateStore by FakePlaybackStateStore() {
            override suspend fun readQueue(): SavedQueue? = throw IllegalStateException("сбой в тесте")
        }
        val callback = testSessionCallback(broken)

        val future = callback.onPlaybackResumption(session(), ownController(), /* isForPlayback = */ true)
        runMainLooperUntil { future.isDone }

        assertTrue(runCatching { future.get() }.exceptionOrNull() is ExecutionException)
    }

    @Test
    fun `сигнал неудачи возобновления вызывается один раз и не выдаётся за сбой чтения`() {
        val uncaught = mutableListOf<Throwable>()
        val callback = PlaybackSessionCallback(
            resumption = PlaybackResumption(FakePlaybackStateStore()),
            scope = CoroutineScope(SupervisorJob() + CoroutineExceptionHandler { _, e -> uncaught += e }),
            mainDispatcher = Dispatchers.Main.immediate,
        )
        var signals = 0
        callback.onResumptionFinished = { succeeded ->
            assertFalse(succeeded)
            signals++
            throw IllegalStateException("сигнал упал в тесте")
        }

        val future = callback.onPlaybackResumption(session(), ownController(), /* isForPlayback = */ true)
        runMainLooperUntil { future.isDone }
        ShadowLooper.idleMainLooper()

        assertEquals(1, signals)
        assertEquals("Нет сохранённой очереди для возобновления", runCatching { future.get() }.exceptionOrNull()?.cause?.message)
        assertTrue(ShadowLog.getLogsForTag("PlaybackSessionCallback").none { it.msg.contains("Could not read") })
        assertEquals(listOf("сигнал упал в тесте"), uncaught.map { it.message })
    }

    /** Решение о подключении принимается сразу: future уже заполнен, главный поток не ждёт. */
    private fun connect(session: MediaSession, controller: ControllerInfo): MediaSession.ConnectionResult {
        val future = callback.onConnectAsync(session, controller)
        assertTrue(future.isDone)
        return future.get()
    }

    /** Полный доступ — все команды сессии и плеера, включая чтение очереди, независимо от доверия системы. */
    private fun assertFullAccess(result: MediaSession.ConnectionResult) {
        assertTrue(result.isAccepted)
        assertTrue(result.availablePlayerCommands.contains(Player.COMMAND_GET_TIMELINE))
        assertEquals(MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS, result.availablePlayerCommands)
        assertEquals(MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS, result.availableSessionCommands)
    }

    private val savedQueue = listOf(
        QueueItem("1", TrackSource.DEEZER, "One", "A", null, 30_000L),
        QueueItem("2", TrackSource.DEEZER, "Two", "B", null, 30_000L),
    )

    private fun savedStore() = FakePlaybackStateStore(
        queue = SavedQueue(savedQueue.map { it.toSaved() }),
        position = SavedPosition(index = 1, key = savedQueue[1].key, positionMs = 42_000L),
    )

    private fun ownController() = controller(
        packageName = RuntimeEnvironment.getApplication().packageName,
        uid = Process.myUid(),
        trusted = true,
    )

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
