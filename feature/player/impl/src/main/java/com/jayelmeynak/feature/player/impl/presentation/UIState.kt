package com.jayelmeynak.feature.player.impl.presentation

import com.jayelmeynak.lib.designsystem.UiText

internal sealed class UIState {
    object Initial : UIState()
    object Ready : UIState()
    object Loading : UIState()
    data class Error(val errorMessage: UiText) : UIState()
}