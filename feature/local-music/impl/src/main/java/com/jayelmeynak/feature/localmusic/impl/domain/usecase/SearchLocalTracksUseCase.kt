package com.jayelmeynak.feature.localmusic.impl.domain.usecase

import com.jayelmeynak.feature.localmusic.impl.domain.model.LocalTrack
import javax.inject.Inject

/** Поиск по загруженным трекам: подстрока названия или исполнителя без учёта регистра, порядок сохраняется. */
internal class SearchLocalTracksUseCase @Inject constructor() {

    operator fun invoke(tracks: List<LocalTrack>, query: String): List<LocalTrack> =
        tracks.filter { track ->
            track.title.contains(query, ignoreCase = true) ||
                track.artistName.contains(query, ignoreCase = true)
        }
}
