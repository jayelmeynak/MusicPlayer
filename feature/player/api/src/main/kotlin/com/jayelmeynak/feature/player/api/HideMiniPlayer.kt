package com.jayelmeynak.feature.player.api

import androidx.navigation3.runtime.NavMetadataKey

/**
 * Ключ метаданных записи навигации: `true` — пока запись показана наверху, мини-плеера нет.
 *
 * Экран объявляет признак при регистрации: `entry<K>(metadata = metadata { put(HideMiniPlayer, true) })`.
 * Без ключа (или с `false`) мини-плеер на месте. Оболочка смотрит только на показанную запись,
 * класс ключа она не проверяет.
 */
public object HideMiniPlayer : NavMetadataKey<Boolean>
