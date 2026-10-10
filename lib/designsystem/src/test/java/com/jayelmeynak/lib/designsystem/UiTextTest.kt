package com.jayelmeynak.lib.designsystem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class UiTextTest {

    @Test
    fun `текст из ресурса без аргументов равен такому же`() {
        assertEquals(
            UiText.StringResourceId(R.string.error_no_internet),
            UiText.StringResourceId(R.string.error_no_internet),
        )
    }

    @Test
    fun `текст из ресурса с одинаковыми аргументами равен такому же`() {
        val first = UiText.StringResourceId(R.string.error_unknown, listOf("a", 1))
        val second = UiText.StringResourceId(R.string.error_unknown, listOf("a", 1))

        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
    }

    @Test
    fun `тексты с разными аргументами или ресурсами не равны`() {
        assertNotEquals(
            UiText.StringResourceId(R.string.error_unknown, listOf("a")),
            UiText.StringResourceId(R.string.error_unknown, listOf("b")),
        )
        assertNotEquals(
            UiText.StringResourceId(R.string.error_unknown),
            UiText.StringResourceId(R.string.error_no_internet),
        )
    }
}
