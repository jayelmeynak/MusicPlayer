package com.jayelmeynak.feature.localmusic.impl.presentation

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.jayelmeynak.feature.localmusic.impl.domain.model.LocalTrack
import com.jayelmeynak.lib.designsystem.UiText
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Вкладка «Local» без состояния. Robolectric — ради Compose-правила с тестовой Activity. */
@RunWith(RobolectricTestRunner::class)
class LocalTracksScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val actions = mutableListOf<LocalTracksAction>()

    private val yesterday = localTrack(1, title = "Yesterday", artist = "The Beatles")
    private val numb = localTrack(2, title = "Numb", artist = "Linkin Park")
    private val help = localTrack(3, title = "Help!", artist = "The Beatles")

    @Test
    fun `медиатека - строки с названием и исполнителем`() {
        setScreen(LocalTracksUiState(library = LibraryState.Loaded(listOf(yesterday, numb))))

        composeRule.onNodeWithText("Yesterday").assertExists()
        composeRule.onNodeWithText("Linkin Park").assertExists()
    }

    @Test
    fun `тап по строке - запуск нарисованного списка с трека`() {
        val tracks = listOf(yesterday, numb, help)
        setScreen(LocalTracksUiState(library = LibraryState.Loaded(tracks)))

        composeRule.onNodeWithText("Numb").performClick()

        assertEquals(listOf<LocalTracksAction>(LocalTracksAction.OnTrackClick(tracks, 1)), actions)
    }

    @Test
    fun `активный поиск - рисуется и запускается выдача`() {
        setScreen(
            LocalTracksUiState(
                query = "help",
                library = LibraryState.Loaded(listOf(yesterday, numb, help)),
                search = LocalSearchState.Results(listOf(help)),
            )
        )

        composeRule.onNodeWithText("Yesterday").assertDoesNotExist()
        composeRule.onNodeWithText("Help!").performClick()

        assertEquals(listOf<LocalTracksAction>(LocalTracksAction.OnTrackClick(listOf(help), 0)), actions)
    }

    @Test
    fun `ввод в строку поиска - смена запроса`() {
        setScreen(LocalTracksUiState(library = LibraryState.Loaded(listOf(yesterday))))

        composeRule.onNode(hasSetTextAction()).performTextInput("numb")

        assertEquals(listOf<LocalTracksAction>(LocalTracksAction.OnSearchQueryChange("numb")), actions)
    }

    @Test
    fun `нет доступа - текст и кнопка, кнопка просит доступ`() {
        setScreen(
            LocalTracksUiState(access = AudioAccess.Denied(permanently = false), library = LibraryState.Loaded(listOf(yesterday)))
        )

        composeRule.onNodeWithText("Нет доступа к аудиофайлам").assertExists()
        composeRule.onNodeWithText("Yesterday").assertDoesNotExist()
        // Вёрстка каталога: строка поиска видна и без доступа.
        composeRule.onNode(hasSetTextAction()).assertExists()
        composeRule.onNodeWithText("Разрешить доступ").performClick()

        assertEquals(listOf<LocalTracksAction>(LocalTracksAction.OnGrantAccessClick), actions)
    }

    @Test
    fun `окончательный отказ - текст про настройки и кнопка «Открыть настройки»`() {
        setScreen(LocalTracksUiState(access = AudioAccess.Denied(permanently = true)))

        composeRule.onNodeWithText("Доступ к аудиофайлам запрещён. Разрешите его в настройках приложения").assertExists()
        composeRule.onNodeWithText("Разрешить доступ").assertDoesNotExist()
        composeRule.onNodeWithText("Открыть настройки").performClick()

        assertEquals(listOf<LocalTracksAction>(LocalTracksAction.OnGrantAccessClick), actions)
    }

    @Test
    fun `пустая выдача - ничего не найдено`() {
        setScreen(
            LocalTracksUiState(
                query = "zzz",
                library = LibraryState.Loaded(listOf(yesterday)),
                search = LocalSearchState.Results(emptyList()),
            )
        )

        composeRule.onNodeWithText("Ничего не найдено").assertExists()
    }

    @Test
    fun `пустая медиатека - текст пустого состояния`() {
        setScreen(LocalTracksUiState(library = LibraryState.Loaded(emptyList())))

        composeRule.onNodeWithText("На устройстве нет аудио").assertExists()
    }

    @Test
    fun `ошибка загрузки - текст ошибки`() {
        setScreen(LocalTracksUiState(library = LibraryState.Failed(UiText.DynamicString("Не удалось загрузить треки"))))

        composeRule.onNodeWithText("Не удалось загрузить треки").assertExists()
        composeRule.onNode(hasSetTextAction()).assertExists()
    }

    @Test
    fun `загрузка - индикатор и строка поиска, строк списка нет`() {
        setScreen(LocalTracksUiState(query = "yes", search = LocalSearchState.Results(listOf(yesterday))))

        composeRule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertExists()
        composeRule.onNode(hasSetTextAction()).assertExists()
        composeRule.onNodeWithText("Yesterday").assertDoesNotExist()
    }

    private fun setScreen(state: LocalTracksUiState) {
        composeRule.setContent {
            LocalTracksScreen(
                state = state,
                onAction = { actions += it },
            )
        }
    }

    private fun localTrack(id: Long, title: String, artist: String) = LocalTrack(
        id = id,
        title = title,
        artistName = artist,
        duration = 200,
        uri = "content://media/external/audio/media/$id",
    )
}
