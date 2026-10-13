package com.jayelmeynak.feature.localmusic.impl.presentation

import com.jayelmeynak.feature.localmusic.impl.domain.model.LocalTrack

internal sealed interface LocalTracksAction {
    data class OnSearchQueryChange(val query: String) : LocalTracksAction

    /** Результат проверки доступа к аудио (при каждом `resume` экрана и после системного диалога). */
    data class OnAudioPermissionChecked(val granted: Boolean) : LocalTracksAction

    /**
     * Тап по треку [index] в [tracks] — списке, который нарисовал экран (медиатека или выдача поиска).
     * Список передаётся, а не берётся из состояния: выдача могла смениться между отрисовкой и тапом.
     */
    data class OnTrackClick(val tracks: List<LocalTrack>, val index: Int) : LocalTracksAction
}
