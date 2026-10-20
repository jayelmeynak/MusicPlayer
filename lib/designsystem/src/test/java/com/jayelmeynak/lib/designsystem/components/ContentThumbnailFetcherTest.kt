package com.jayelmeynak.lib.designsystem.components

import android.graphics.Bitmap
import android.net.Uri
import android.os.CancellationSignal
import android.os.OperationCanceledException
import coil3.BitmapImage
import coil3.fetch.ImageFetchResult
import coil3.request.Options
import coil3.size.Size
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.FileNotFoundException
import android.util.Size as PlatformSize

/** Robolectric: `Uri`, `Bitmap`, ресурсы. Эскиз провайдера подменяется — MediaProvider в тесте нет. */
@RunWith(RobolectricTestRunner::class)
class ContentThumbnailFetcherTest {

    private val context = RuntimeEnvironment.getApplication()
    private val uri = Uri.parse("content://media/external/audio/media/7")
    private val bitmap: Bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
    private val requests = mutableListOf<Pair<Uri, PlatformSize>>()

    @Test
    fun `эскиз запрашивается по URI в размере слота, результат помечен уменьшенным`() = runTest {
        val result = fetcher(Size(120, 80)) { requestedUri, size, _ ->
            requests += requestedUri to size
            bitmap
        }.fetch() as ImageFetchResult

        assertEquals(listOf(uri to PlatformSize(120, 80)), requests)
        assertTrue(result.isSampled)
        assertSame(bitmap, (result.image as BitmapImage).bitmap)
    }

    @Test
    fun `размер слота неизвестен - эскиз размера по умолчанию`() = runTest {
        fetcher(Size.ORIGINAL) { requestedUri, size, _ ->
            requests += requestedUri to size
            bitmap
        }.fetch()

        val default = ContentThumbnailFetcher.DEFAULT_SIZE_PX
        assertEquals(listOf(uri to PlatformSize(default, default)), requests)
    }

    @Test
    fun `нет эскиза - ошибка загрузки уходит в Coil`() {
        val fetcher = fetcher(Size(10, 10)) { _, _, _ -> throw FileNotFoundException("нет эскиза") }

        assertThrows(FileNotFoundException::class.java) { runTest { fetcher.fetch() } }
    }

    @Test
    fun `отмена во время загрузки - сигнал провайдеру отменён, наружу отмена, а не ошибка`() = runBlocking {
        val started = CompletableDeferred<CancellationSignal>()
        val fetch = async(Dispatchers.IO) {
            fetcher(Size(10, 10)) { _, _, signal ->
                started.complete(signal)
                // Как провайдер: прерывание потока не замечает, ждёт сигнала и бросает его исключение.
                while (!signal.isCanceled) {
                    try {
                        Thread.sleep(5)
                    } catch (_: InterruptedException) {
                    }
                }
                throw OperationCanceledException()
            }.fetch()
        }
        val signal = started.await()

        fetch.cancel()

        try {
            fetch.await()
            fail("загрузка не отменена")
        } catch (_: CancellationException) {
        }
        assertTrue(signal.isCanceled)
    }

    private fun fetcher(size: Size, load: ThumbnailLoader) =
        ContentThumbnailFetcher(uri, Options(context, size = size), load)
}
