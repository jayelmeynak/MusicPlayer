package com.jayelmeynak.musicplayer.testing

/**
 * Сессия пакета из вывода `dumpsys media_session`: состояние воспроизведения и описание метаданных.
 * Формат состояния зависит от API: `state=3` (29) или `state=PLAYING(3)` (33+) — берётся число.
 */
data class SessionDump(val state: Int?, val description: String?, val raw: String) {

    companion object {
        private val STATE = Regex("""state=PlaybackState \{state=(?:[A-Z_]+\()?(\d+)""")
        private val DESCRIPTION = Regex("""description=(.*)""")

        private val BUTTON_SESSION = Regex("""Media button session is (\S+)""")
        private val RECEIVER_PACKAGE = Regex("""ComponentInfo\{([^/}]+)/|pkg=([\w.]+)|PendingIntent\{\S+: \S+ ([\w.]+)""")

        /**
         * Пакет, которому система отдаст следующую медиакнопку: сессия медиакнопок, а без неё —
         * последний (или восстановленный после перезагрузки) приёмник медиакнопок. `null` —
         * адресата нет или формат вывода не распознан.
         */
        fun mediaButtonTarget(dump: String): String? {
            BUTTON_SESSION.find(dump)?.groupValues?.get(1)
                ?.takeIf { it != "null" }
                ?.let { return it.substringBefore('/') }
            val lines = dump.lines()
            val receiver = lines.firstOrNull { it.trim().startsWith("Last MediaButtonReceiver:") }
                ?.substringAfter(':')?.trim()?.takeIf { it != "null" }
                ?: lines.firstOrNull { it.trim().startsWith("Restored MediaButtonReceiver:") }
                    ?.substringAfter(':')?.trim()?.takeIf { it != "null" }
                ?: return null
            return RECEIVER_PACKAGE.find(receiver)?.groupValues?.drop(1)?.firstOrNull { it.isNotEmpty() }
        }

        fun parse(dump: String, packageName: String): SessionDump {
            val lines = dump.lines()
            val start = lines.indexOfFirst { it.trim() == "package=$packageName" }
            if (start < 0) return SessionDump(state = null, description = null, raw = "")
            val end = lines.drop(start + 1).indexOfFirst { it.trim().startsWith("package=") }
                .let { if (it < 0) lines.size else start + 1 + it }
            val block = lines.subList(start, end).joinToString("\n")
            return SessionDump(
                state = STATE.find(block)?.groupValues?.get(1)?.toInt(),
                description = DESCRIPTION.find(block)?.groupValues?.get(1)?.trim(),
                raw = block,
            )
        }
    }
}
