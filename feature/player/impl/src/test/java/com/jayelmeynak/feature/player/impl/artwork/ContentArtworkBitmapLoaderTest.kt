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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.ExecutionException

/** Robolectric: `Bitmap`, `Uri` и `MediaMetadata` — классы Android. */
@RunWith(RobolectricTestRunner::class)
class ContentArtworkBitmapLoaderTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val delegate = RecordingBitmapLoader()
    private val thumbnail: Bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
    private val thumbnails = FakeContentThumbnailSource(mapOf(LOCAL_ID to thumbnail))
    private val loader = ContentArtworkBitmapLoader(delegate, thumbnails, CoroutineScope(dispatcher), dispatcher)

    @Test
    fun `content-обложка - эскиз провайдера не больше предела сессии`() {
        val metadata = MediaMetadata.Builder().setArtworkUri(Uri.parse(LOCAL_ID)).build()

        val bitmap = loader.loadBitmapFromMetadata(metadata)!!.get()

        assertSame(thumbnail, bitmap)
        assertEquals(listOf(LOCAL_ID to ContentArtworkBitmapLoader.MAX_ARTWORK_SIZE_PX), thumbnails.requested)
        assertTrue(delegate.fromMetadata.isEmpty())
    }

    @Test
    fun `обложка по https грузится стандартным загрузчиком`() {
        val metadata = MediaMetadata.Builder().setArtworkUri(Uri.parse("https://example.com/c.jpg")).build()

        loader.loadBitmapFromMetadata(metadata)

        assertEquals(listOf(metadata), delegate.fromMetadata)
        assertTrue(thumbnails.requested.isEmpty())
    }

    @Test
    fun `данные обложки в метаданных - стандартный загрузчик, эскиз не запрашивается`() {
        val metadata = MediaMetadata.Builder()
            .setArtworkUri(Uri.parse(LOCAL_ID))
            .setArtworkData(byteArrayOf(1), MediaMetadata.PICTURE_TYPE_FRONT_COVER)
            .build()

        loader.loadBitmapFromMetadata(metadata)

        assertEquals(listOf(metadata), delegate.fromMetadata)
        assertTrue(thumbnails.requested.isEmpty())
    }

    @Test
    fun `нет эскиза - future с ошибкой`() {
        val metadata = MediaMetadata.Builder()
            .setArtworkUri(Uri.parse("content://media/external/audio/media/99"))
            .build()

        val future = loader.loadBitmapFromMetadata(metadata)!!

        assertTrue(runCatching { future.get() }.exceptionOrNull() is ExecutionException)
    }

    @Test
    fun `отмена future отменяет загрузку эскиза`() {
        thumbnails.pending[LOCAL_ID] = CompletableDeferred()
        val metadata = MediaMetadata.Builder().setArtworkUri(Uri.parse(LOCAL_ID)).build()

        loader.loadBitmapFromMetadata(metadata)!!.cancel(false)

        assertEquals(listOf(LOCAL_ID), thumbnails.cancelled)
    }

    @Test
    fun `эскиз крупнее предела сессии дожимается с сохранением пропорций`() {
        thumbnails.thumbnails = mapOf(LOCAL_ID to Bitmap.createBitmap(3_000, 2_000, Bitmap.Config.ARGB_8888))
        val metadata = MediaMetadata.Builder().setArtworkUri(Uri.parse(LOCAL_ID)).build()

        val bitmap = loader.loadBitmapFromMetadata(metadata)!!.get()

        assertEquals(ContentArtworkBitmapLoader.MAX_ARTWORK_SIZE_PX, maxOf(bitmap.width, bitmap.height))
        assertEquals(3f / 2f, bitmap.width.toFloat() / bitmap.height, 0.01f)
    }

    @Test
    fun `загрузка по content URI напрямую - тот же эскиз провайдера`() {
        val bitmap = loader.loadBitmap(Uri.parse(LOCAL_ID)).get()

        assertSame(thumbnail, bitmap)
        assertEquals(listOf(LOCAL_ID to ContentArtworkBitmapLoader.MAX_ARTWORK_SIZE_PX), thumbnails.requested)
    }

    @Test
    fun `загрузка по https напрямую - стандартный загрузчик`() {
        val bitmap = loader.loadBitmap(Uri.parse("https://example.com/c.jpg")).get()

        assertSame(delegate.bitmap, bitmap)
        assertTrue(thumbnails.requested.isEmpty())
    }

    private class RecordingBitmapLoader : BitmapLoader {
        val bitmap: Bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        val fromMetadata = mutableListOf<MediaMetadata>()

        override fun supportsMimeType(mimeType: String): Boolean = true

        override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> = Futures.immediateFuture(bitmap)

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
