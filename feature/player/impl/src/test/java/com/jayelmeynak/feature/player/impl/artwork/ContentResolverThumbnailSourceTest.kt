package com.jayelmeynak.feature.player.impl.artwork

import android.graphics.Bitmap
import android.net.Uri
import android.os.CancellationSignal
import android.os.OperationCanceledException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.FileNotFoundException
import android.util.Size as PlatformSize

/** Robolectric: `Uri`, `Bitmap`, `CancellationSignal`. Загрузка эскиза подменена — MediaProvider в тесте нет. */
@RunWith(RobolectricTestRunner::class)
class ContentResolverThumbnailSourceTest {

    private val uri = Uri.parse("content://media/external/audio/media/7")

    @Test
    fun `эскиз запрашивается квадратом предела`() = runBlocking {
        val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        val requests = mutableListOf<Pair<Uri, PlatformSize>>()
        val source = ContentResolverThumbnailSource { requested, size, _ ->
            requests += requested to size
            bitmap
        }

        assertSame(bitmap, source.thumbnail(uri, 1_024))
        assertEquals(listOf(uri to PlatformSize(1_024, 1_024)), requests)
    }

    @Test
    fun `нет эскиза - ошибка провайдера пробрасывается`() {
        val source = ContentResolverThumbnailSource { _, _, _ -> throw FileNotFoundException("нет эскиза") }

        assertThrows(FileNotFoundException::class.java) { runBlocking { source.thumbnail(uri, 1_024) } }
    }

    @Test
    fun `отмена во время загрузки - сигнал провайдеру отменён, наружу отмена, а не ошибка`() = runBlocking {
        val started = CompletableDeferred<CancellationSignal>()
        val source = ContentResolverThumbnailSource { _, _, signal ->
            started.complete(signal)
            // Как провайдер: прерывание потока не замечает, ждёт сигнала и бросает его исключение.
            while (!signal.isCanceled) {
                try {
                    Thread.sleep(5)
                } catch (_: InterruptedException) {
                }
            }
            throw OperationCanceledException()
        }
        val load = async(Dispatchers.IO) { source.thumbnail(uri, 1_024) }
        val signal = started.await()

        load.cancel()

        try {
            load.await()
            fail("загрузка не отменена")
        } catch (_: CancellationException) {
        }
        assertTrue(signal.isCanceled)
    }
}
