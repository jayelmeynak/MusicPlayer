package com.jayelmeynak.lib.designsystem.components

import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/**
 * Слайдер прогресса воспроизведения в процентах. Во время перетаскивания следует за пальцем и
 * игнорирует [progress]; [onSeek] срабатывает один раз, с итоговым значением, когда жест кончается;
 * отменённый жест перематывает к последней позиции пальца. Новый [trackKey] сбрасывает незаконченный
 * жест, чтобы тот не перемотал следующий трек.
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
