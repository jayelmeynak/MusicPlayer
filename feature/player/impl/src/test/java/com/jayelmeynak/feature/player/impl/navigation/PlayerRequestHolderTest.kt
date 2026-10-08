package com.jayelmeynak.feature.player.impl.navigation

import com.jayelmeynak.feature.player.api.TrackSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayerRequestHolderTest {

    private val holder = PlayerRequestHolder()

    @Test
    fun `без запроса забирать нечего`() {
        assertNull(holder.take())
    }

    @Test
    fun `запрос отдаётся плееру`() {
        holder.open(TrackSource.DEEZER, "42")

        assertEquals(PlayerRequest(TrackSource.DEEZER, "42"), holder.take())
    }

    @Test
    fun `запрос отдаётся один раз`() {
        holder.open(TrackSource.LOCAL, "content://media/1")
        holder.take()

        assertNull(holder.take())
    }

    @Test
    fun `побеждает последний запрос`() {
        holder.open(TrackSource.DEEZER, "1")
        holder.open(TrackSource.LOCAL, "content://media/2")

        assertEquals(PlayerRequest(TrackSource.LOCAL, "content://media/2"), holder.take())
    }
}
