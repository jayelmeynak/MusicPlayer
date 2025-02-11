package com.jayelmeynak.ui

import androidx.compose.runtime.Composable
import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource

sealed interface UiText {
    data class DynamicString(val value: String) : UiText

    class StringResourceId(
        @StringRes val id: Int,
        vararg val args: Any
    ) : UiText

    @Composable
    fun asString(): String {
        return when (this) {
            is DynamicString -> value
            is StringResourceId -> stringResource(id, *args)
        }
    }
}