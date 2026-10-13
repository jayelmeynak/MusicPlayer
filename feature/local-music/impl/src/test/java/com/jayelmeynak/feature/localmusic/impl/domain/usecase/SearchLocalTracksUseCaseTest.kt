package com.jayelmeynak.feature.localmusic.impl.domain.usecase

import com.jayelmeynak.feature.localmusic.impl.domain.model.LocalTrack
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchLocalTracksUseCaseTest {

    private val yesterday = track(1, title = "Yesterday", artist = "The Beatles")
    private val numb = track(2, title = "Numb", artist = "Linkin Park")
    private val help = track(3, title = "Help!", artist = "The Beatles")
    private val tracks = listOf(yesterday, numb, help)

    private val search = SearchLocalTracksUseCase()

    @Test
    fun `подстрока названия без учёта регистра`() = runTest {
        assertEquals(listOf(numb), search(tracks, "NUM"))
    }

    @Test
    fun `исполнитель без учёта регистра, порядок списка сохраняется`() = runTest {
        assertEquals(listOf(yesterday, help), search(tracks, "beatles"))
    }

    @Test
    fun `нет совпадений - пустой список`() = runTest {
        assertEquals(emptyList<LocalTrack>(), search(tracks, "zzzzzz"))
    }

    private fun track(id: Long, title: String, artist: String) = LocalTrack(
        id = id,
        title = title,
        artistName = artist,
        duration = 200,
        uri = "content://media/external/audio/media/$id",
    )
}
