package com.jayelmeynak.feature.player.impl.artwork

import android.graphics.Bitmap
import android.net.Uri
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.BitmapLoader
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.ExecutionException

/** Robolectric: Bitmap, Uri and MediaMetadata are Android classes. */
@RunWith(RobolectricTestRunner::class)
class LocalArtworkBitmapLoaderTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val delegate = RecordingBitmapLoader()
    private val artworks = FakeLocalArtworkSource(mapOf(LOCAL_ID to byteArrayOf(1, 2, 3)))
    private val loader = LocalArtworkBitmapLoader(delegate, artworks, CoroutineScope(dispatcher), dispatcher)

    @Test
    fun `обложка локального трека берётся из кэша обложек по его content URI`() {
        val metadata = MediaMetadata.Builder().setArtworkUri(Uri.parse(LOCAL_ID)).build()

        val bitmap = loader.loadBitmapFromMetadata(metadata)!!.get()

        assertSame(delegate.bitmap, bitmap)
        assertEquals(listOf(LOCAL_ID), artworks.requested)
        assertArrayEquals(byteArrayOf(1, 2, 3), delegate.decoded.single())
    }

    @Test
    fun `обложка по https грузится стандартным загрузчиком`() {
        val metadata = MediaMetadata.Builder().setArtworkUri(Uri.parse("https://example.com/c.jpg")).build()

        loader.loadBitmapFromMetadata(metadata)

        assertEquals(listOf(metadata), delegate.fromMetadata)
        assertTrue(artworks.requested.isEmpty())
    }

    @Test
    fun `локальный трек без обложки - future с ошибкой, без декодирования`() {
        val metadata = MediaMetadata.Builder()
            .setArtworkUri(Uri.parse("content://media/external/audio/media/99"))
            .build()

        val future = loader.loadBitmapFromMetadata(metadata)!!

        assertTrue(runCatching { future.get() }.exceptionOrNull() is ExecutionException)
        assertTrue(delegate.decoded.isEmpty())
    }

    @Test
    fun `отмена future отменяет загрузку обложки`() {
        artworks.pending[LOCAL_ID] = CompletableDeferred()
        val metadata = MediaMetadata.Builder().setArtworkUri(Uri.parse(LOCAL_ID)).build()

        loader.loadBitmapFromMetadata(metadata)!!.cancel(false)

        assertEquals(listOf(LOCAL_ID), artworks.cancelled)
    }

    private class RecordingBitmapLoader : BitmapLoader {
        val bitmap: Bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        val decoded = mutableListOf<ByteArray>()
        val fromMetadata = mutableListOf<MediaMetadata>()

        override fun supportsMimeType(mimeType: String): Boolean = true

        override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> {
            decoded += data
            return Futures.immediateFuture(bitmap)
        }

        override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> = Futures.immediateFuture(bitmap)

        override fun loadBitmapFromMetadata(metadata: MediaMetadata): ListenableFuture<Bitmap>? {
            fromMetadata += metadata
            return Futures.immediateFuture(bitmap)
        }
    }

    private companion object {
        const val LOCAL_ID = "content://media/external/audio/media/7"
    }
}
