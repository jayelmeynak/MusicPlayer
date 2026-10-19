package com.jayelmeynak.feature.player.impl.service

import android.os.Bundle
import android.os.Handler
import androidx.media3.session.MediaSession
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Ключ extras сессии: `true`, пока новая сессия поднимает сохранённую очередь. Контроллер UI не
 * показывает снапшот такой сессии: пустая очередь в нём ещё не значит «играть нечего».
 */
internal const val EXTRA_SESSION_RESTORING = "com.jayelmeynak.player.SESSION_RESTORING"

internal fun sessionExtras(restoring: Boolean): Bundle = Bundle().apply {
    putBoolean(EXTRA_SESSION_RESTORING, restoring)
}

/**
 * Сообщает контроллерам, что восстановление закончено. Изменения плеера Media3 рассылает следующим
 * сообщением лупера, а extras — сразу; поэтому extras уходят через ход лупера после них, иначе
 * контроллер увидел бы «готово» раньше восстановленной очереди и показал бы пустую. Отмена
 * (уничтожение сервиса) до этого хода extras не трогает.
 */
internal suspend fun MediaSession.finishRestoring() {
    awaitLooperTurn(Handler(player.applicationLooper))
    setSessionExtras(sessionExtras(restoring = false))
}

private suspend fun awaitLooperTurn(handler: Handler) = suspendCancellableCoroutine { continuation ->
    val resume = Runnable { continuation.resume(Unit) }
    handler.post(resume)
    continuation.invokeOnCancellation { handler.removeCallbacks(resume) }
}
