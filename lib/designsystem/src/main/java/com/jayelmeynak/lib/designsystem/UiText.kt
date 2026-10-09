package com.jayelmeynak.lib.designsystem

import androidx.compose.runtime.Composable
import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource

public sealed interface UiText {
    public data class DynamicString(public val value: String) : UiText

    /** Текст из ресурса; равенство — по ресурсу и аргументам. */
    public data class StringResourceId(
        @StringRes public val id: Int,
        public val args: List<Any> = emptyList(),
    ) : UiText

    @Composable
    public fun asString(): String {
        return when (this) {
            is DynamicString -> value
            is StringResourceId -> stringResource(id, *args.toTypedArray())
        }
    }
}