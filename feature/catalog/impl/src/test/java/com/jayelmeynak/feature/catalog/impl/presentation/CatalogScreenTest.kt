package com.jayelmeynak.feature.catalog.impl.presentation

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.jayelmeynak.feature.catalog.impl.domain.model.Track
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Строка трека каталога (ADR-15). Robolectric — ради Compose-правила с тестовой Activity. */
@RunWith(RobolectricTestRunner::class)
class CatalogScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val actions = mutableListOf<CatalogAction>()

    private val withLink = Track(
        id = 1L, title = "Talk To You", artistName = "ANOTR", coverUrl = "", duration = 185_000L,
        link = "https://www.deezer.com/track/1",
    )
    private val withoutLink = Track(
        id = 2L, title = "GO!", artistName = "CORTIS", coverUrl = "", duration = 0L, link = null,
    )

    @Test
    fun `строка показывает исполнителя с длительностью и пометку превью`() {
        setScreen(listOf(withLink, withoutLink))

        composeRule.onNodeWithText("ANOTR · 3:05").assertExists()
        composeRule.onNodeWithText("CORTIS").assertExists()
        assertEquals(2, composeRule.onAllNodesWithText(PREVIEW_LABEL).fetchSemanticsNodes().size)
    }

    @Test
    fun `иконка открытия в Deezer - только у трека со ссылкой`() {
        setScreen(listOf(withLink, withoutLink))

        assertEquals(
            1,
            composeRule.onAllNodesWithContentDescription("в Deezer", substring = true).fetchSemanticsNodes().size,
        )
        composeRule.onNodeWithContentDescription("Открыть «Talk To You» в Deezer").assertExists()
    }

    @Test
    fun `тап по иконке - открыть ссылку, не запуск трека`() {
        setScreen(listOf(withLink))

        composeRule.onNodeWithContentDescription("«Talk To You»", substring = true).performClick()

        assertEquals(listOf<CatalogAction>(CatalogAction.OnOpenInDeezerClick("https://www.deezer.com/track/1")), actions)
    }

    @Test
    fun `тап по строке - запуск нарисованного списка с трека`() {
        val tracks = listOf(withLink, withoutLink)
        setScreen(tracks)

        composeRule.onNodeWithText("GO!").performClick()

        assertEquals(listOf<CatalogAction>(CatalogAction.OnTrackClick(tracks, 1)), actions)
    }

    private fun setScreen(tracks: List<Track>) {
        composeRule.setContent {
            CatalogScreen(
                state = CatalogUiState(chart = ChartState.Loaded(tracks)),
                onAction = { actions += it },
            )
        }
    }

    private companion object {
        /** Между числом и единицей — неразрывный пробел: «30 с» не разрывается переносом. */
        const val PREVIEW_LABEL = "Превью 30\u00A0с"
    }
}
