package com.jayelmeynak.lib.designsystem.search

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged

/** Пауза после ввода, после которой запускается поиск. */
public const val SEARCH_DEBOUNCE_MS: Long = 500L

/**
 * Запросы поиска по вводу: непустой — после паузы [SEARCH_DEBOUNCE_MS], пустой — сразу (очистка
 * строки отменяет идущий поиск без задержки); тот же запрос подряд не повторяется. Потребитель —
 * `collectLatest`, чтобы новый запрос отменял прежний.
 */
@OptIn(FlowPreview::class)
public fun Flow<String>.debounceSearchQuery(): Flow<String> =
    debounce { query -> if (query.isBlank()) 0L else SEARCH_DEBOUNCE_MS }
        .distinctUntilChanged()
