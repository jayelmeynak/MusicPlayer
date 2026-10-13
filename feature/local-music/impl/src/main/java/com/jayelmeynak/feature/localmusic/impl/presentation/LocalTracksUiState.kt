package com.jayelmeynak.feature.localmusic.impl.presentation

import com.jayelmeynak.feature.localmusic.impl.domain.model.LocalTrack
import com.jayelmeynak.lib.designsystem.UiText

/**
 * Состояние вкладки «Local»: введённый запрос, доступ к аудио и исходы загрузки медиатеки и поиска.
 * Что показать, вычисляется из них ([content]), а не хранится отдельными флагами.
 *
 * @property artworks обложки треков по id; `null` — у трека нет обложки, нет ключа — ещё грузится.
 */
internal data class LocalTracksUiState(
    val query: String = "",
    val access: AudioAccess = AudioAccess.Unknown,
    val library: LibraryState = LibraryState.Loading,
    val search: LocalSearchState = LocalSearchState.Inactive,
    val artworks: Map<Long, ByteArray?> = emptyMap(),
) {
    /**
     * Нет доступа — важнее всего; затем загрузка и ошибка медиатеки; затем выдача поиска (пустая —
     * «ничего не найдено»), а без поиска — медиатека (пустая — своё пустое состояние). Пустой запрос —
     * поиск неактивен сразу, не дожидаясь, пока VM сбросит выдачу: очищенная строка не показывает
     * прежнюю выдачу ни в одном кадре.
     */
    val content: LocalTracksContent
        get() = when {
            access == AudioAccess.Denied -> LocalTracksContent.NoAccess
            else -> when (library) {
                LibraryState.Loading -> LocalTracksContent.Loading
                is LibraryState.Failed -> LocalTracksContent.Error(library.message)
                is LibraryState.Loaded -> loadedContent(library.tracks)
            }
        }

    private fun loadedContent(libraryTracks: List<LocalTrack>): LocalTracksContent {
        val results = (search as? LocalSearchState.Results)?.takeUnless { query.isBlank() }
        return when {
            results == null ->
                if (libraryTracks.isEmpty()) LocalTracksContent.Empty else LocalTracksContent.Tracks(libraryTracks)

            results.tracks.isEmpty() -> LocalTracksContent.NothingFound
            else -> LocalTracksContent.Tracks(results.tracks)
        }
    }
}

/** Доступ к аудио устройства; `Unknown` — проверки ещё не было. */
internal sealed interface AudioAccess {
    data object Unknown : AudioAccess
    data object Granted : AudioAccess
    data object Denied : AudioAccess
}

/** Исход загрузки треков устройства. */
internal sealed interface LibraryState {
    data object Loading : LibraryState
    data class Loaded(val tracks: List<LocalTrack>) : LibraryState
    data class Failed(val message: UiText) : LibraryState
}

/** Поиск по медиатеке; `Inactive` — запрос пуст или поиск по нему ещё не применён (пауза ввода). */
internal sealed interface LocalSearchState {
    data object Inactive : LocalSearchState
    data class Results(val tracks: List<LocalTrack>) : LocalSearchState
}

/** Что показывает вкладка «Local». */
internal sealed interface LocalTracksContent {
    data object NoAccess : LocalTracksContent
    data object Loading : LocalTracksContent
    data class Error(val message: UiText) : LocalTracksContent
    data object NothingFound : LocalTracksContent
    /** Медиатека пуста и поиск неактивен. */
    data object Empty : LocalTracksContent
    data class Tracks(val tracks: List<LocalTrack>) : LocalTracksContent
}
