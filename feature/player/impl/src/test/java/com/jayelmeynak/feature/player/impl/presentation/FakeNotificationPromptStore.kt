package com.jayelmeynak.feature.player.impl.presentation

import com.jayelmeynak.feature.player.impl.permission.NotificationPromptStore
import kotlinx.coroutines.CompletableDeferred

/**
 * Флаг «уже спрашивали разрешение уведомлений» в памяти. [gate] — пока не завершён, ответ на
 * чтение флага задерживается (медленный IO-раунд).
 */
internal class FakeNotificationPromptStore(
    var prompted: Boolean = false,
    val gate: CompletableDeferred<Unit>? = null,
) : NotificationPromptStore {
    override suspend fun isPrompted(): Boolean {
        // Значение прочитано сразу, ответ приходит после [gate]: как чтение файла на IO.
        val value = prompted
        gate?.await()
        return value
    }

    override suspend fun markPrompted() {
        prompted = true
    }
}
