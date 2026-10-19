package com.jayelmeynak.feature.player.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Экран плеера: показывает текущий элемент [PlaybackController]. Чтобы сыграть список, вызови
 * [PlaybackController.play], затем перейди сюда.
 */
@Serializable
public data object PlayerDestination : NavKey
