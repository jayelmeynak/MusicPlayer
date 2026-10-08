package com.jayelmeynak.lib.network.data.dto

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DtoParsingTest {

    private val gson = Gson()

    @Test
    fun `пустой объект разбирается в каждый DTO с null-полями`() {
        assertEquals(TrackDto(), gson.fromJson("{}", TrackDto::class.java))
        assertEquals(AlbumDto(), gson.fromJson("{}", AlbumDto::class.java))
        assertEquals(Artist(), gson.fromJson("{}", Artist::class.java))
        assertEquals(Tracks(), gson.fromJson("{}", Tracks::class.java))
        assertEquals(ResponseChart(), gson.fromJson("{}", ResponseChart::class.java))
    }

    @Test
    fun `трек без вложенных объектов - artist и album null`() {
        val track = gson.fromJson("""{"id":1,"title":"t"}""", TrackDto::class.java)

        assertNull(track.artist)
        assertNull(track.album)
        assertNull(track.preview)
    }

    @Test
    fun `список с пустым треком - один TrackDto с null-полями`() {
        val tracks = gson.fromJson("""{"data":[{}]}""", Tracks::class.java)

        assertEquals(listOf(TrackDto()), tracks.tracks)
    }

    @Test
    fun `чарт без data - tracks внутри null`() {
        val chart = gson.fromJson("""{"tracks":{}}""", ResponseChart::class.java)

        assertNull(chart.tracks?.tracks)
    }
}
