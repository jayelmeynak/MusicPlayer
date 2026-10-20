package com.jayelmeynak.lib.designsystem.components

import android.graphics.Bitmap
import coil3.SingletonImageLoader
import coil3.decode.DataSource
import coil3.request.CachePolicy
import coil3.request.SuccessResult
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream

/**
 * Обложка по адресу грузится общим загрузчиком Coil, которым рисует `AsyncImage`: сеть в Coil 3 —
 * отдельный артефакт, без него запрос по URL падает. Native-графика Robolectric — чтобы PNG декодировался.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TrackArtworkNetworkTest {

    private val context = RuntimeEnvironment.getApplication()
    private val server = MockWebServer()

    @Before
    fun setUp() {
        server.start()
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `обложка по адресу загружается из сети`() = runBlocking {
        server.enqueue(MockResponse.Builder().body(Buffer().write(png(width = 3, height = 2))).build())
        val url = server.url("/cover.png").toString()

        // Кэши выключены: дисковый кэш Coil живёт во временном каталоге хоста и пережил бы прогон.
        val request = trackArtworkRequest(context, url).newBuilder()
            .diskCachePolicy(CachePolicy.DISABLED)
            .memoryCachePolicy(CachePolicy.DISABLED)
            .build()
        val result = SingletonImageLoader.get(context).execute(request)

        assertTrue("ожидался успех, а не $result", result is SuccessResult)
        result as SuccessResult
        assertEquals(DataSource.NETWORK, result.dataSource)
        assertEquals(3 to 2, result.image.width to result.image.height)
        assertEquals("/cover.png", server.takeRequest().url.encodedPath)
    }

    private fun png(width: Int, height: Int): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        return ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.toByteArray()
        }
    }
}
