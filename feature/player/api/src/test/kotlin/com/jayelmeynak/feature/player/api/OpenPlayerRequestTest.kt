package com.jayelmeynak.feature.player.api

import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenPlayerRequestTest {

    @Test
    fun `запрос ведёт на экран плеера`() {
        assertSame(PlayerDestination, OpenPlayerRequest.destination)
    }

    @Test
    fun `действие в пространстве имён фичи плеера`() {
        assertTrue(OpenPlayerRequest.ACTION.startsWith("com.jayelmeynak.feature.player."))
    }
}
