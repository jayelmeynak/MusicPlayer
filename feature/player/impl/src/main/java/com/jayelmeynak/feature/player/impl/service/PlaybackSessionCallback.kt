package com.jayelmeynak.feature.player.impl.service

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.jayelmeynak.feature.player.impl.persistence.PlaybackResumption
import com.jayelmeynak.util.coroutines.ApplicationScope
import com.jayelmeynak.util.coroutines.Dispatcher
import com.jayelmeynak.util.coroutines.MusicPlayerDispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import java.util.Collections
import javax.inject.Inject

/**
 * Decides who may connect to the session and accepts queue items from controllers.
 *
 * Connections follow [access]: this app and controllers trusted by the system get every command,
 * everyone else is rejected, so no other app reads the queue (the user's library) or drives the
 * player.
 *
 * Items come without a URI (see [toMediaItem]); each one gets the internal track URI, and
 * [TrackUriDataSpecResolver] asks the resolver of its source for the playable URL right before
 * loading it. A URI that comes with an item is never opened.
 *
 * Возобновление ([onPlaybackResumption]) отдаёт сохранённую очередь. Media3 зовёт его на play при
 * пустом плеере — от принятого контроллера (по политике выше) или от медиакнопки, пришедшей в
 * сервис через приёмник: это системный путь, а не подключение, нового уровня доступа нет.
 */
@OptIn(UnstableApi::class)
internal class PlaybackSessionCallback @Inject constructor(
    private val resumption: PlaybackResumption,
    @ApplicationScope private val scope: CoroutineScope,
    @Dispatcher(MusicPlayerDispatchers.Main) private val mainDispatcher: CoroutineDispatcher,
) : MediaSession.Callback {

    /**
     * Исход возобновления для воспроизведения (`isForPlayback = true`), один раз на запрос, на главном
     * потоке, после того как future заполнен: `false` — возобновлять нечего, Media3 сыграет пустой
     * плеер и foreground не начнётся. Сервис, запущенный медиакнопкой, при неудаче останавливается сам.
     */
    var onResumptionFinished: ((succeeded: Boolean) -> Unit)? = null

    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
    ): MediaSession.ConnectionResult = when (controller.access()) {
        ControllerAccess.FULL -> MediaSession.ConnectionResult.AcceptedResultBuilder(session).build()
        ControllerAccess.NONE -> {
            // A platform controller is asked again on every command (e.g. each media key press).
            if (loggedRejectedUids.add(controller.uid)) {
                Log.w(TAG, "Rejected controller ${controller.packageName} (uid=${controller.uid})")
            }
            MediaSession.ConnectionResult.reject()
        }
    }

    override fun onAddMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: MutableList<MediaItem>,
    ): ListenableFuture<MutableList<MediaItem>> =
        Futures.immediateFuture(prepareMediaItems(mediaItems).toMutableList())

    /**
     * Сохранённая очередь для play без текущего элемента: [isForPlayback] — вся очередь, индекс и
     * позиция, иначе только текущий элемент. Нечего возобновлять (очереди нет, файл повреждён, все
     * источники неизвестны) или чтение упало — future с ошибкой: Media3 залогирует её и вызовет
     * play на пустом плеере (ничего не заиграет); при [isForPlayback] затем зовётся [onResumptionFinished].
     */
    override fun onPlaybackResumption(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        isForPlayback: Boolean,
    ): ListenableFuture<MediaItemsWithStartPosition> {
        val future = SettableFuture.create<MediaItemsWithStartPosition>()
        scope.launch(mainDispatcher) {
            // В try — только чтение: сбой сигнала исхода не выдаётся за сбой чтения и не повторяется.
            var readError: Exception? = null
            val items = try {
                resumption.load(isForPlayback)
            } catch (e: CancellationException) {
                future.cancel(false)
                throw e
            } catch (e: Exception) {
                // Сбой чтения не роняет процесс: Media3 получит ошибку и сыграет то, что есть (ничего).
                Log.w(TAG, "Could not read the saved queue", e)
                readError = e
                null
            }
            if (items != null) {
                future.set(items)
            } else {
                future.setException(readError ?: IllegalStateException("Нет сохранённой очереди для возобновления"))
            }
            if (isForPlayback) onResumptionFinished?.invoke(items != null)
        }
        return future
    }

    /**
     * An item of this app gets the internal URI of its track. Any other item gets an internal URI
     * that never resolves, even if it carries its own URI: playing a `file://`, an arbitrary
     * `content://` provider or a URL as is would open it with this app's permissions. Loading such
     * an item fails with a player error (the player stops in IDLE with the error, it does not skip
     * to the next item), and the rest of the queue stays in place.
     */
    fun prepareMediaItems(mediaItems: List<MediaItem>): List<MediaItem> = mediaItems.map { it.withTrackUri() }

    private companion object {
        const val TAG = "PlaybackSessionCallback"

        /** Uids whose rejection is already logged in this process: one line per uid, not per key. */
        val loggedRejectedUids: MutableSet<Int> = Collections.synchronizedSet(HashSet())
    }
}
