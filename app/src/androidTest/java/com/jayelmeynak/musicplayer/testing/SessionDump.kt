package com.jayelmeynak.musicplayer.testing

/**
 * Сессия пакета из вывода `dumpsys media_session`: состояние воспроизведения и описание метаданных.
 * Формат состояния зависит от API: `state=3` (29) или `state=PLAYING(3)` (33+) — берётся число.
 */
data class SessionDump(val state: Int?, val description: String?, val raw: String) {

    companion object {
        private val STATE = Regex("""state=PlaybackState \{state=(?:[A-Z_]+\()?(\d+)""")
        private val DESCRIPTION = Regex("""description=(.*)""")

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
