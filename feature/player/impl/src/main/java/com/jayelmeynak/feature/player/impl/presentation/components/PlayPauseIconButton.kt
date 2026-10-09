package com.jayelmeynak.feature.player.impl.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Play/pause с кольцом прогресса, пока текущий элемент загружается. Иконка показывает, что сделает
 * тап ([showPlayButton]), поэтому во время буферизации это пауза: воспроизведение уже запрошено.
 */
@Composable
internal fun PlayPauseIconButton(
    showPlayButton: Boolean,
    isBuffering: Boolean,
    onIconButtonClick: () -> Unit,
) {
    Box(contentAlignment = Alignment.Center) {
        if (isBuffering) {
            CircularProgressIndicator(
                modifier = Modifier.size(RING_SIZE),
                strokeWidth = 2.dp,
            )
        }
        AnimatedContent(
            targetState = showPlayButton,
            transitionSpec = {
                scaleIn() togetherWith scaleOut()
            },
        ) { showPlay ->
            // Уходящий и входящий контент рисуют каждый своё состояние: иконка и описание
            // совпадают и во время анимации.
            IconButton(onClick = onIconButtonClick) {
                Icon(
                    imageVector = if (showPlay) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                    contentDescription = if (showPlay) "Воспроизведение" else "Пауза",
                )
            }
        }
    }
}

private val RING_SIZE = 44.dp
