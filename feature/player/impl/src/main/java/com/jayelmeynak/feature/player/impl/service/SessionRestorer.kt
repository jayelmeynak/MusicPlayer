package com.jayelmeynak.feature.player.impl.service

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import com.jayelmeynak.feature.player.impl.persistence.PlaybackResumption
import com.jayelmeynak.util.coroutines.ApplicationScope
import com.jayelmeynak.util.coroutines.Dispatcher
import com.jayelmeynak.util.coroutines.MusicPlayerDispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import javax.inject.Inject

/**
 * Поднимает в новой сессии сохранённую очередь — на паузе, без `prepare`: звука и уведомления
 * нет, пока пользователь не нажмёт play (тогда Media3 сам вызовет `prepare`). Очередь, которую
 * контроллер успел задать раньше, не перетирается. Конец восстановления сессия сообщает в extras
 * ([EXTRA_SESSION_RESTORING] = `false`).
 */
@OptIn(UnstableApi::class)
internal class SessionRestorer @Inject constructor(
    private val resumption: PlaybackResumption,
    @ApplicationScope private val scope: CoroutineScope,
    @Dispatcher(MusicPlayerDispatchers.Main) private val mainDispatcher: CoroutineDispatcher,
) {

    /** Отменяется в `onDestroy` сервиса до освобождения сессии. */
    fun restore(session: MediaSession): Job = scope.launch(mainDispatcher) {
        val saved = try {
            resumption.load(forPlayback = true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Без восстановления сессия всё равно должна сообщить «готово», иначе UI ждёт вечно.
            Log.w(TAG, "Could not read the saved queue", e)
            null
        }
        val player = session.player
        if (saved != null && player.mediaItemCount == 0) {
            player.setMediaItems(saved.mediaItems, saved.startIndex, saved.startPositionMs)
        }
        session.finishRestoring()
    }

    private companion object {
        const val TAG = "SessionRestorer"
    }
}
