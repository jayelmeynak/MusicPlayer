package com.jayelmeynak.player.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable


@Composable
fun PlayPauseIconButton(
    isPlaying: Boolean,
    onIconButtonClick: () -> Unit
) {
    AnimatedContent(
        targetState = isPlaying,
        transitionSpec = {
            scaleIn() togetherWith scaleOut()
        }
    ) { isPlay ->
        IconButton(
            onClick = {
                onIconButtonClick()
            }
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlay) "Пауза" else "Воспроизведение"
            )
        }
    }
}