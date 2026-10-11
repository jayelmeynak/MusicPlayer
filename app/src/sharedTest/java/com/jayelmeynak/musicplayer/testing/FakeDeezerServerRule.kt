package com.jayelmeynak.musicplayer.testing

import org.junit.rules.ExternalResource

/**
 * Запускает [FakeDeezerServer] до запуска Activity и обнуляет счётчики запросов.
 * Ставить раньше правила Activity: адрес нужен при первой инъекции.
 */
class FakeDeezerServerRule : ExternalResource() {
    override fun before() {
        FakeDeezerServer.start()
        FakeDeezerServer.resetCounters()
    }
}
