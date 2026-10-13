package com.jayelmeynak.feature.localmusic.impl.presentation

/** Одноразовые сигналы вкладки «Local». */
internal sealed interface LocalTracksEffect {
    /** Очередь уже отдана плееру: открыть экран плеера. */
    data object OpenPlayer : LocalTracksEffect

    /** Показать системный диалог доступа к аудио. */
    data object RequestAudioPermission : LocalTracksEffect

    /** Диалог больше не покажется: открыть настройки приложения. */
    data object OpenAppSettings : LocalTracksEffect
}
