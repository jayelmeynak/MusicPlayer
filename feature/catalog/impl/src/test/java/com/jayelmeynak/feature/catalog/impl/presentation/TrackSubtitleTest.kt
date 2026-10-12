package com.jayelmeynak.feature.catalog.impl.presentation

import org.junit.Assert.assertEquals
import org.junit.Test

class TrackSubtitleTest {

    @Test
    fun `исполнитель и полная длина трека через точку`() {
        assertEquals("ANOTR · 3:05", formatTrackSubtitle("ANOTR", 185_000L))
    }

    @Test
    fun `длительность неизвестна - только исполнитель`() {
        assertEquals("ANOTR", formatTrackSubtitle("ANOTR", 0L))
    }

    @Test
    fun `исполнителя нет - только время`() {
        assertEquals("3:05", formatTrackSubtitle("", 185_000L))
    }

    @Test
    fun `трек длиннее часа - минуты целиком`() {
        assertEquals("A · 75:00", formatTrackSubtitle("A", 75 * 60_000L))
    }
}
