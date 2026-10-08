package com.jayelmeynak.feature.player.impl.artwork

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.BitmapLoader
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSourceBitmapLoader
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.google.common.util.concurrent.SettableFuture
import com.jayelmeynak.util.coroutines.ApplicationScope
import com.jayelmeynak.util.coroutines.Dispatcher
import com.jayelmeynak.util.coroutines.MusicPlayerDispatchers
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

/**
 * Bitmap loader of the session (notification, system UI). A local track's artwork URI is the
 * `content://` URI of the audio file, which no image decoder reads: its cover comes from the
 * artwork cache instead, loaded off the main thread and only for the item being shown. Anything
 * else goes to [delegate].
 */
@OptIn(UnstableApi::class)
internal class LocalArtworkBitmapLoader(
    private val delegate: BitmapLoader,
    private val artworkSource: LocalArtworkSource,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
) : BitmapLoader {

    @Inject
    constructor(
        @ApplicationContext context: Context,
        artworkSource: LocalArtworkSource,
        @ApplicationScope scope: CoroutineScope,
        @Dispatcher(MusicPlayerDispatchers.IO) ioDispatcher: CoroutineDispatcher,
    ) : this(DataSourceBitmapLoader.Builder(context).build(), artworkSource, scope, ioDispatcher)

    override fun supportsMimeType(mimeType: String): Boolean = delegate.supportsMimeType(mimeType)

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> = delegate.decodeBitmap(data)

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> = delegate.loadBitmap(uri)

    override fun loadBitmapFromMetadata(metadata: MediaMetadata): ListenableFuture<Bitmap>? {
        val uri = metadata.artworkUri
        if (metadata.artworkData != null || uri?.scheme != ContentResolver.SCHEME_CONTENT) {
            return delegate.loadBitmapFromMetadata(metadata)
        }
        val result = SettableFuture.create<Bitmap>()
        val job = scope.launch(ioDispatcher) {
            try {
                val data = artworkSource.artwork(uri.toString())
                if (data == null) {
                    result.setException(IOException("No artwork for $uri"))
                } else {
                    result.setFuture(delegate.decodeBitmap(data))
                }
            } catch (e: CancellationException) {
                result.cancel(false)
                throw e
            } catch (e: Exception) {
                result.setException(e)
            }
        }
        // Media3 cancels the future when the item changes: stop reading the cache for it.
        result.addListener({ if (result.isCancelled) job.cancel() }, MoreExecutors.directExecutor())
        return result
    }
}
