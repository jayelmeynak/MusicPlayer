package com.jayelmeynak.lib.designsystem

import androidx.compose.runtime.Composable
import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource

public sealed interface UiText {
    public data class DynamicString(public val value: String) : UiText

    public class StringResourceId(
        @StringRes public val id: Int,
        public vararg val args: Any
    ) : UiText

    @Composable
    public fun asString(): String {
        return when (this) {
            is DynamicString -> value
            is StringResourceId -> stringResource(id, *args)
        }
    }
}