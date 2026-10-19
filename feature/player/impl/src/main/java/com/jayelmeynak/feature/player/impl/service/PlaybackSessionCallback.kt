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
 * Решает, кто может подключиться к сессии, и принимает элементы очереди от контроллеров.
 *
 * Подключения следуют [access]: это приложение и контроллеры, которым доверяет система, получают все
 * команды, остальные отклоняются — так ни одно другое приложение не читает очередь (библиотеку
 * пользователя) и не управляет плеером.
 *
 * Элементы приходят без URI (см. [toMediaItem]); каждый получает внутренний URI трека, а
 * [TrackUriDataSpecResolver] прямо перед загрузкой просит у резолвера его источника играбельный URL.
 * URI, пришедший вместе с элементом, никогда не открывается.
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
            // Платформенный контроллер спрашивается заново на каждую команду
            // (например, на каждое нажатие медиаклавиши).
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
     * Элемент этого приложения получает внутренний URI своего трека. Любой другой — внутренний URI,
     * который никогда не резолвится, даже если у элемента есть свой URI: сыграть `file://`,
     * произвольный провайдер `content://` или URL как есть значило бы открыть их с разрешениями этого
     * приложения. Загрузка такого элемента падает ошибкой плеера (плеер встаёт в IDLE с ошибкой и не
     * переходит к следующему элементу), остальная очередь остаётся на месте.
     */
    fun prepareMediaItems(mediaItems: List<MediaItem>): List<MediaItem> = mediaItems.map { it.withTrackUri() }

    private companion object {
        const val TAG = "PlaybackSessionCallback"

        /** Uid, чей отказ уже залогирован в этом процессе: одна строка на uid, а не на каждую клавишу. */
        val loggedRejectedUids: MutableSet<Int> = Collections.synchronizedSet(HashSet())
    }
}
