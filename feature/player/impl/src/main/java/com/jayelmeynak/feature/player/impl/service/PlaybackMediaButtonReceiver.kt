package com.jayelmeynak.feature.player.impl.service

import android.content.Context
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaButtonReceiver
import com.jayelmeynak.feature.player.impl.persistence.hasSavedQueue

/**
 * Медиакнопка (гарнитура, Bluetooth) при неработающем сервисе — например, после смерти процесса:
 * Media3 стартует сервис плеера в foreground, сессия возобновляет сохранённую очередь
 * (`PlaybackSessionCallback.onPlaybackResumption`).
 *
 * Без сохранённой очереди (файла нет или очередь в нём пуста) сервис не стартует: играть нечего,
 * а foreground-сервис без воспроизведения система уронит (`ForegroundServiceDidNotStartInTimeException`).
 * Проверяется только размер файла, без чтения DataStore: приёмник работает на главном потоке, и
 * stat одного файла в каталоге приложения — осознанное дисковое обращение на main (см.
 * `hasSavedQueue`). Повреждённый файл или очередь с неизвестными источниками проверку проходят —
 * тогда возобновление отказывает, и сервис останавливается сам (`PlayBackService`).
 *
 * Приёмник экспортирован (так его находит система). Явный broadcast от чужого приложения может
 * запустить play последнего трека, но доступа к очереди и командам не даёт — принятый риск.
 */
@OptIn(UnstableApi::class)
internal class PlaybackMediaButtonReceiver : MediaButtonReceiver() {

    override fun shouldStartForegroundService(context: Context, intent: Intent): Boolean =
        context.hasSavedQueue()
}
