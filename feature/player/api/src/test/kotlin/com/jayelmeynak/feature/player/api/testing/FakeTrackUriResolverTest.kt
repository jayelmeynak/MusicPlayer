package com.jayelmeynak.feature.player.api.testing

import com.jayelmeynak.feature.player.api.TrackSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** `requested` и `source` фейка проверяют тесты загрузки треков в `player:impl`. */
class FakeTrackUriResolverTest {

    @Test
    fun `отдаёт ссылку по id, null для остальных, записывает запрошенные id и называет источник`() = runTest {
        val resolver = FakeTrackUriResolver(TrackSource.LOCAL, mapOf("1" to "content://1", "2" to null))

        assertEquals("content://1", resolver.resolve("1"))
        assertNull(resolver.resolve("2"))
        assertNull(resolver.resolve("3"))
        assertEquals(listOf("1", "2", "3"), resolver.requested)
        assertEquals(TrackSource.LOCAL, resolver.source)
    }
}
