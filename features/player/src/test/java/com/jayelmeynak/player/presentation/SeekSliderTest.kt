package com.jayelmeynak.player.presentation

import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import com.jayelmeynak.player.presentation.components.SeekSlider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Robolectric hosts the Compose test rule: the project has no instrumented tests.
@RunWith(RobolectricTestRunner::class)
class SeekSliderTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val seeks = mutableListOf<Float>()
    private val progress = mutableFloatStateOf(0f)
    private val trackKey = mutableLongStateOf(1L)

    @Test
    fun `перетаскивание - одна перемотка на точку отпускания`() {
        setSlider(progress = 10f)

        slider().performTouchInput {
            down(centerLeft)
            moveTo(center)
            moveTo(centerRight)
        }
        assertTrue(seeks.isEmpty())

        slider().performTouchInput { up() }

        assertEquals(1, seeks.size)
        assertTrue("seek=${seeks.single()}", seeks.single() > 90f)
    }

    @Test
    fun `во время жеста ползунок показывает позицию пальца, а не прогресс плеера`() {
        setSlider(progress = 10f)

        // The first move only passes the touch slop; the slider follows from the second one.
        slider().performTouchInput {
            down(centerLeft)
            moveTo(percentOffset(0.3f, 0.5f))
            moveTo(center)
        }

        val shown = sliderValue()
        assertTrue("shown=$shown", shown in 40f..60f)
        slider().performTouchInput { up() }
    }

    @Test
    fun `после жеста ползунок снова следует за прогрессом плеера`() {
        setSlider(progress = 10f)

        slider().performTouchInput {
            down(centerLeft)
            moveTo(percentOffset(0.3f, 0.5f))
            moveTo(center)
            up()
        }
        progress.value = 55f

        assertEquals(55f, sliderValue())
    }

    @Test
    fun `смена трека посреди жеста - перемотки нет`() {
        setSlider(progress = 10f)

        slider().performTouchInput {
            down(centerLeft)
            moveTo(percentOffset(0.3f, 0.5f))
            moveTo(center)
        }
        trackKey.value = 2L
        composeRule.waitForIdle()
        slider().performTouchInput { up() }

        assertTrue("seeks=$seeks", seeks.isEmpty())
    }

    @Test
    fun `тап без перетаскивания - одна перемотка`() {
        setSlider(progress = 10f)

        slider().performTouchInput { click(center) }

        assertEquals(1, seeks.size)
        assertTrue("seek=${seeks.single()}", seeks.single() in 40f..60f)
    }

    private fun setSlider(progress: Float) {
        this.progress.value = progress
        composeRule.setContent {
            SeekSlider(
                progress = this.progress.value,
                onSeek = { seeks += it },
                modifier = Modifier.testTag(TAG),
                trackKey = trackKey.value,
            )
        }
    }

    private fun slider() = composeRule.onNodeWithTag(TAG)

    private fun sliderValue(): Float =
        slider().fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].current

    private companion object {
        const val TAG = "seek"
    }
}
