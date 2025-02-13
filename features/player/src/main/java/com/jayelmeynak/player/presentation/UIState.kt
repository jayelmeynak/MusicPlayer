package com.jayelmeynak.player.presentation

import com.jayelmeynak.ui.UiText

sealed class UIState {
    object Initial : UIState()
    object Ready : UIState()
    object Loading : UIState()
    data class Error(val errorMessage: UiText) : UIState()
}