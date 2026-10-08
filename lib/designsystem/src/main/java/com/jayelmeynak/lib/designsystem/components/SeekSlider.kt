package com.jayelmeynak.lib.designsystem.components

import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/**
 * Playback progress slider in percent. While dragged it follows the finger and ignores [progress];
 * [onSeek] fires once, with the final value, when the gesture ends; a cancelled gesture seeks to
 * the last finger position. A new [trackKey] drops an unfinished gesture, so it can't seek the next track.
 */
@Composable
public fun SeekSlider(
    progress: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    trackKey: Any? = null,
) {
    var dragValue by remember(trackKey) { mutableStateOf<Float?>(null) }

    Slider(
        value = dragValue ?: progress,
        onValueChange = { dragValue = it },
        onValueChangeFinished = {
            dragValue?.let(onSeek)
            dragValue = null
        },
        valueRange = 0f..100f,
        modifier = modifier,
    )
}
