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
 * жест, чтобы тот не перемотал следующий трек. Заблокированный ([enabled] = `false`, перемотка
 * недоступна) не принимает касаний и рисуется как неактивный слайдер Material3; блокировка посреди
 * жеста отменяет его без перемотки.
 */
@Composable
public fun SeekSlider(
    progress: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    trackKey: Any? = null,
) {
    // Блокировка посреди жеста сбрасывает его: отпускание уже не дойдёт до слайдера.
    var dragValue by remember(trackKey, enabled) { mutableStateOf<Float?>(null) }

    Slider(
        value = dragValue ?: progress,
        onValueChange = { dragValue = it },
        onValueChangeFinished = {
            dragValue?.let(onSeek)
            dragValue = null
        },
        modifier = modifier,
        enabled = enabled,
        valueRange = 0f..100f,
    )
}
