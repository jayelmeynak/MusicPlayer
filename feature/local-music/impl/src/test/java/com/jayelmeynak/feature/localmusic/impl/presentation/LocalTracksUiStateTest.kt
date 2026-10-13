package com.jayelmeynak.feature.localmusic.impl.presentation

import com.jayelmeynak.feature.localmusic.impl.domain.model.LocalTrack
import com.jayelmeynak.lib.designsystem.UiText
import org.junit.Assert.assertEquals
import org.junit.Test

/** Правила [LocalTracksUiState.content]: что показывает вкладка «Local» при данных исходах. */
class LocalTracksUiStateTest {

    private val yesterday = localTrack(1, "Yesterday")
    private val help = localTrack(3, "Help!")
    private val error = UiText.DynamicString("ошибка")

    @Test
    fun `нет доступа важнее загрузки, ошибки, выдачи и треков`() {
        val libraries = listOf(LibraryState.Loading, LibraryState.Failed(error), LibraryState.Loaded(listOf(yesterday)))
        libraries.forEach { library ->
            val state = LocalTracksUiState(
                access = AudioAccess.Denied(permanently = false),
                library = library,
                search = LocalSearchState.Results(emptyList()),
            )
            assertEquals(LocalTracksContent.NoAccess(permanently = false), state.content)
        }
    }

    @Test
    fun `окончательный отказ - нет доступа с признаком окончательного`() {
        val state = LocalTracksUiState(
            access = AudioAccess.Denied(permanently = true),
            library = LibraryState.Loaded(listOf(yesterday)),
        )

        assertEquals(LocalTracksContent.NoAccess(permanently = true), state.content)
    }

    @Test
    fun `начальное состояние - загрузка`() {
        assertEquals(LocalTracksContent.Loading, LocalTracksUiState().content)
    }

    @Test
    fun `загрузка важнее выдачи`() {
        val state = LocalTracksUiState(search = LocalSearchState.Results(emptyList()))

        assertEquals(LocalTracksContent.Loading, state.content)
    }

    @Test
    fun `ошибка загрузки - текст ошибки, даже при пустой выдаче`() {
        val state = LocalTracksUiState(
            library = LibraryState.Failed(error),
            search = LocalSearchState.Results(emptyList()),
        )

        assertEquals(LocalTracksContent.Error(error), state.content)
    }

    @Test
    fun `пустая выдача - ничего не найдено`() {
        val state = LocalTracksUiState(
            query = "zzz",
            library = LibraryState.Loaded(listOf(yesterday)),
            search = LocalSearchState.Results(emptyList()),
        )

        assertEquals(LocalTracksContent.NothingFound, state.content)
    }

    @Test
    fun `пустая медиатека при активном поиске - ничего не найдено, а не пустая медиатека`() {
        val state = LocalTracksUiState(
            query = "zzz",
            library = LibraryState.Loaded(emptyList()),
            search = LocalSearchState.Results(emptyList()),
        )

        assertEquals(LocalTracksContent.NothingFound, state.content)
    }

    @Test
    fun `пустая медиатека без поиска - пустое состояние`() {
        val state = LocalTracksUiState(library = LibraryState.Loaded(emptyList()))

        assertEquals(LocalTracksContent.Empty, state.content)
    }

    @Test
    fun `без поиска - треки медиатеки`() {
        val state = LocalTracksUiState(library = LibraryState.Loaded(listOf(yesterday, help)))

        assertEquals(LocalTracksContent.Tracks(listOf(yesterday, help)), state.content)
    }

    @Test
    fun `активный поиск - выдача вместо медиатеки`() {
        val state = LocalTracksUiState(
            query = "help",
            library = LibraryState.Loaded(listOf(yesterday, help)),
            search = LocalSearchState.Results(listOf(help)),
        )

        assertEquals(LocalTracksContent.Tracks(listOf(help)), state.content)
    }

    @Test
    fun `очищенный запрос - медиатека, даже если выдача ещё не сброшена`() {
        val state = LocalTracksUiState(
            query = "",
            library = LibraryState.Loaded(listOf(yesterday, help)),
            search = LocalSearchState.Results(listOf(help)),
        )

        assertEquals(LocalTracksContent.Tracks(listOf(yesterday, help)), state.content)
    }

    @Test
    fun `очищенный запрос и пустая выдача - медиатека, а не «ничего не найдено»`() {
        val state = LocalTracksUiState(
            query = "  ",
            library = LibraryState.Loaded(listOf(yesterday)),
            search = LocalSearchState.Results(emptyList()),
        )

        assertEquals(LocalTracksContent.Tracks(listOf(yesterday)), state.content)
    }

    @Test
    fun `доступ не проверен или выдан - на содержимое не влияет`() {
        listOf(AudioAccess.Unknown, AudioAccess.Granted).forEach { access ->
            val state = LocalTracksUiState(access = access, library = LibraryState.Loaded(listOf(yesterday)))
            assertEquals(LocalTracksContent.Tracks(listOf(yesterday)), state.content)
        }
    }

    private fun localTrack(id: Long, title: String) = LocalTrack(
        id = id,
        title = title,
        artistName = "",
        duration = 200,
        uri = "content://media/external/audio/media/$id",
    )
}
