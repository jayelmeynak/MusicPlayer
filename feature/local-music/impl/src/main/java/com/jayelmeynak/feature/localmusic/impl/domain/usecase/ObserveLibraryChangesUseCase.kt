package com.jayelmeynak.feature.localmusic.impl.domain.usecase

import com.jayelmeynak.feature.localmusic.impl.domain.repository.LocalTracksRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import javax.inject.Inject

/** Пауза после последнего изменения медиатеки, после которой список перечитывается. */
internal const val LIBRARY_CHANGES_DEBOUNCE_MS = 1_000L

/**
 * Изменения медиатеки, после которых стоит перечитать список: сканер шлёт пачку уведомлений на
 * каждый файл, поэтому пачка схлопывается в одно событие после паузы [LIBRARY_CHANGES_DEBOUNCE_MS].
 */
internal class ObserveLibraryChangesUseCase @Inject constructor(
    private val repository: LocalTracksRepository,
) {
    @OptIn(FlowPreview::class)
    operator fun invoke(): Flow<Unit> = repository.libraryChanges().debounce(LIBRARY_CHANGES_DEBOUNCE_MS)
}
