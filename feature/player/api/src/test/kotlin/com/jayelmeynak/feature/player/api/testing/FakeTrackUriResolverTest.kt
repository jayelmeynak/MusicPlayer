package com.jayelmeynak.feature.player.api.testing

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FakeTrackUriResolverTest {

    @Test
    fun `returns the mapped uri, null otherwise, and records requested ids`() = runTest {
        val resolver = FakeTrackUriResolver(mapOf("1" to "https://example.com/1.mp3", "2" to null))

        assertEquals("https://example.com/1.mp3", resolver.resolve("1"))
        assertNull(resolver.resolve("2"))
        assertNull(resolver.resolve("3"))
        assertEquals(listOf("1", "2", "3"), resolver.requested)
    }
}
