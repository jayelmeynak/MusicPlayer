package com.jayelmeynak.feature.localmusic.impl.presentation

import com.jayelmeynak.feature.localmusic.impl.domain.model.LocalTrack
import com.jayelmeynak.lib.designsystem.UiText

internal data class LocalTracksState(
    val tracks: List<LocalTrack> = emptyList(),
    val artworks: Map<Long, ByteArray?> = emptyMap(),
    val isLoading: Boolean = true,
    val errorMessage: UiText? = null,
    val query: String = "",
    /** `null`, пока поиск не активен; пустой, если поиск ничего не нашёл. */
    val searchList: List<LocalTrack>? = null,
    val isPermissionDenied: Boolean = false,
) {
    /** Доступ есть, загрузка завершена без ошибки, треков нет, поиск не активен. */
    val isLibraryEmpty: Boolean
        get() = !isPermissionDenied && !isLoading && errorMessage == null &&
            searchList == null && tracks.isEmpty()
}
