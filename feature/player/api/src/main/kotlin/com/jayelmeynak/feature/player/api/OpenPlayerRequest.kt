package com.jayelmeynak.feature.player.api

import androidx.navigation3.runtime.NavKey

/**
 * Запрос «открыть экран плеера» снаружи приложения — тап по уведомлению плеера.
 *
 * Сессия плеера ставит [ACTION] в явный интент `MainActivity` (`sessionActivity`, неизменяемый
 * `PendingIntent`, без extras). Activity, получив интент с этим действием в новом запуске или в
 * `onNewIntent`, передаёт оболочке [destination], и та делает `navigateTo` — экран плеера встаёт
 * наверх текущего стека. Extras интента не читаются.
 */
public object OpenPlayerRequest {

    /** Действие интента `MainActivity`. */
    public const val ACTION: String = "com.jayelmeynak.feature.player.action.OPEN_PLAYER"

    /** Ключ экрана, который открывает запрос: оболочке не нужно знать класс ключа плеера. */
    public val destination: NavKey = PlayerDestination
}
