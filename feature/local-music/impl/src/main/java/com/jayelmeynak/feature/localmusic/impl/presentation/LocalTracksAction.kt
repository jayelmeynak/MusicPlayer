package com.jayelmeynak.feature.localmusic.impl.presentation

import com.jayelmeynak.feature.localmusic.impl.domain.model.LocalTrack

internal sealed interface LocalTracksAction {
    data class OnSearchQueryChange(val query: String) : LocalTracksAction
    data class OnAudioPermissionChecked(val granted: Boolean) : LocalTracksAction

    /** Тап по треку: [tracks] — список, который нарисовал экран (весь или выдача поиска). */
    data class OnTrackClick(val tracks: List<LocalTrack>, val index: Int) : LocalTracksAction
}
