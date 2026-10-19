package com.jayelmeynak.lib.designsystem.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Robolectric — ради Compose-правила с тестовой Activity.
@RunWith(RobolectricTestRunner::class)
class TrackListTest {

    @get:Rule
    val composeRule = createComposeRule()

    private data class Song(val id: Int, val name: String, val singer: String)

    private val songs = listOf(Song(1, "Первая", "А"), Song(2, "Вторая", "Б"), Song(3, "Третья", "В"))

    @Test
    fun `строки показывают название и исполнителя каждого трека`() {
        setList { _, _ -> }

        songs.forEach { song ->
            composeRule.onNodeWithText(song.name).assertExists()
            composeRule.onNodeWithText(song.singer).assertExists()
        }
    }

    @Test
    fun `тап по строке отдаёт нарисованный список и индекс трека`() {
        val clicks = mutableListOf<Pair<List<Song>, Int>>()
        setList { tracks, index -> clicks += tracks to index }

        composeRule.onNodeWithText("Вторая").performClick()

        assertEquals(listOf(songs to 1), clicks)
    }

    private fun setList(onTrackClick: (List<Song>, Int) -> Unit) {
        composeRule.setContent {
            TrackList(
                tracks = songs,
                key = { it.id },
                title = { it.name },
                artistName = { it.singer },
                onTrackClick = onTrackClick,
                image = {},
            )
        }
    }

    @Test
    fun `дополнительная строка и элемент справа показаны, тап по элементу не запускает трек`() {
        val clicks = mutableListOf<Int>()
        val trailingClicks = mutableListOf<String>()
        composeRule.setContent {
            TrackList(
                tracks = songs,
                key = { it.id },
                title = { it.name },
                artistName = { it.singer },
                onTrackClick = { _, index -> clicks += index },
                supportingText = { "Пометка ${it.id}" },
                trailing = { song ->
                    IconButton(onClick = { trailingClicks += song.name }) {
                        Icon(Icons.Filled.Star, contentDescription = "Действие ${song.name}")
                    }
                },
                image = {},
            )
        }

        composeRule.onNodeWithText("Пометка 2").assertExists()
        composeRule.onNodeWithContentDescription("Действие Вторая").performClick()

        assertEquals(listOf("Вторая"), trailingClicks)
        assertEquals(emptyList<Int>(), clicks)
    }

    @Test
    fun `без дополнительной строки и элемента справа - строка как раньше`() {
        setList { _, _ -> }

        composeRule.onNodeWithText("Пометка 1").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Действие Первая").assertDoesNotExist()
    }
}
