package com.jayelmeynak.feature.catalog.impl.presentation

import java.util.Locale

/**
 * Подпись строки трека каталога: «Исполнитель · M:SS» по полной длине трека. Длина 0 (неизвестна)
 * не показывается; без исполнителя — только время.
 */
internal fun formatTrackSubtitle(artistName: String, durationMs: Long): String {
    val length = if (durationMs > 0) formatLength(durationMs) else null
    return listOfNotNull(artistName.ifBlank { null }, length).joinToString(SEPARATOR)
}

private fun formatLength(durationMs: Long): String {
    val totalSeconds = durationMs / MILLIS_IN_SECOND
    return String.format(Locale.ROOT, "%d:%02d", totalSeconds / SECONDS_IN_MINUTE, totalSeconds % SECONDS_IN_MINUTE)
}

private const val SEPARATOR = " · "
private const val MILLIS_IN_SECOND = 1000L
private const val SECONDS_IN_MINUTE = 60L
