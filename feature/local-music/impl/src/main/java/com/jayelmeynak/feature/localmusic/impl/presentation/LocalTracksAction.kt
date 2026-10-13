package com.jayelmeynak.feature.localmusic.impl.presentation

import com.jayelmeynak.feature.localmusic.impl.domain.model.LocalTrack

internal sealed interface LocalTracksAction {
    data class OnSearchQueryChange(val query: String) : LocalTracksAction

    /** Проверка доступа к аудио при каждом `resume` экрана (в т.ч. после возврата из настроек). */
    data class OnAudioPermissionChecked(val granted: Boolean) : LocalTracksAction

    /**
     * Исход системного запроса доступа и `shouldShowRequestPermissionRationale` в момент запроса
     * ([rationaleBeforeRequest]) и после ответа ([rationaleAfterRequest]). Окончательный ли отказ,
     * решает VM: экран только читает системные значения.
     */
    data class OnAudioPermissionResult(
        val granted: Boolean,
        val rationaleBeforeRequest: Boolean,
        val rationaleAfterRequest: Boolean,
    ) : LocalTracksAction

    /** Кнопка на экране «Нет доступа». */
    data object OnGrantAccessClick : LocalTracksAction

    /**
     * Тап по треку [index] в [tracks] — списке, который нарисовал экран (медиатека или выдача поиска).
     * Список передаётся, а не берётся из состояния: выдача могла смениться между отрисовкой и тапом.
     */
    data class OnTrackClick(val tracks: List<LocalTrack>, val index: Int) : LocalTracksAction
}
