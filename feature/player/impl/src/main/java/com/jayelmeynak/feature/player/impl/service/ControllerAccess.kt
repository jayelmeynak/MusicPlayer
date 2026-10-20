package com.jayelmeynak.feature.player.impl.service

import android.os.Process
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession

/**
 * Может ли контроллер подключиться к сессии воспроизведения.
 *
 * Очередь сессии — видимый список экрана, вплоть до всей локальной библиотеки пользователя (названия,
 * исполнители, `content://` id), поэтому читать её могут только контроллеры, которым доверяет
 * пользователь или система.
 *
 * Доступ намеренно двоичный. Любое принятое подключение, даже с урезанным набором команд, отдаёт
 * контроллеру токен платформенной сессии, а платформенная сессия публикует текущие метаданные и
 * очередь каждому держателю токена в обход набора команд. Уровень «только транспорт» снова отдал бы
 * очередь. Медиакнопки при живой сессии идут через систему; возобновление после смерти процесса —
 * через [PlaybackMediaButtonReceiver], а не через новый уровень доступа.
 */
internal enum class ControllerAccess {
    /** Все команды, включая чтение очереди и её метаданных. */
    FULL,

    /** Подключение отклонено: ни команд, ни чтения. */
    NONE,
}

/**
 * Политика доступа к сессии воспроизведения:
 * - это приложение (тот же uid): контроллер UI и контроллер уведомления Media3;
 * - контроллеры, которым доверяет система ([MediaSession.ControllerInfo.isTrusted]): System UI,
 *   системный диспетчер медиаклавиш, Bluetooth, приложения с `MEDIA_CONTENT_CONTROL` или с
 *   включённым notification listener. Media3 проверяет это по uid и pid вызывающего. У
 *   Media3-контроллера другого uid имя пакета ещё должно быть проверено по этому uid; платформенные
 *   (legacy) контроллеры проверить можно не всегда, их судят только по доверию;
 * - все остальные, включая платформенные контроллеры, получают [ControllerAccess.NONE]. Клавиши
 *   гарнитуры и Bluetooth и шторка уведомлений доходят до сессии через контроллер уведомления или
 *   систему — они покрыты выше; недоверенное приложение может получить токен платформенной сессии,
 *   только подключившись к этому сервису как browser, и этот путь тоже спрашивает политику.
 *   Сторонние переназначатели медиаклавиш, шлющие клавиши от себя (а не от системы), тоже
 *   отклоняются.
 *
 * Читает только поля [MediaSession.ControllerInfo]: для платформенных контроллеров `onConnectAsync`
 * может блокировать главный поток.
 */
@OptIn(UnstableApi::class)
internal fun MediaSession.ControllerInfo.access(ownUid: Int = Process.myUid()): ControllerAccess = when {
    uid == ownUid -> ControllerAccess.FULL
    !isTrusted -> ControllerAccess.NONE
    controllerVersion == MediaSession.ControllerInfo.LEGACY_CONTROLLER_VERSION -> ControllerAccess.FULL
    isPackageNameVerified -> ControllerAccess.FULL
    else -> ControllerAccess.NONE
}
