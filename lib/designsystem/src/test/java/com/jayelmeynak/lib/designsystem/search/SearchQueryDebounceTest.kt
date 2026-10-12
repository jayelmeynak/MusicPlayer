package com.jayelmeynak.lib.designsystem.search

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchQueryDebounceTest {

    private val input = Channel<String>(Channel.UNLIMITED)
    private val output = mutableListOf<String>()

    @Test
    fun `непустой запрос проходит только после паузы ввода`() = runTest {
        backgroundScope.launch { input.receiveAsFlow().debounceSearchQuery().collect { output += it } }
        input.send("abc")
        runCurrent()

        advanceTimeBy(SEARCH_DEBOUNCE_MS - 1)
        assertEquals(emptyList<String>(), output)

        advanceTimeBy(2)
        assertEquals(listOf("abc"), output)
    }

    @Test
    fun `ввод внутри паузы - проходит только последний запрос`() = runTest {
        backgroundScope.launch { input.receiveAsFlow().debounceSearchQuery().collect { output += it } }
        input.send("a")
        advanceTimeBy(100)
        input.send("ab")
        advanceTimeBy(SEARCH_DEBOUNCE_MS + 1)

        assertEquals(listOf("ab"), output)
    }

    @Test
    fun `пустой запрос проходит сразу, без паузы`() = runTest {
        backgroundScope.launch { input.receiveAsFlow().debounceSearchQuery().collect { output += it } }
        input.send("abc")
        advanceTimeBy(SEARCH_DEBOUNCE_MS + 1)
        input.send("")
        runCurrent()

        assertEquals(listOf("abc", ""), output)
    }

    @Test
    fun `тот же запрос подряд не проходит повторно`() = runTest {
        backgroundScope.launch { input.receiveAsFlow().debounceSearchQuery().collect { output += it } }
        input.send("abc")
        advanceTimeBy(SEARCH_DEBOUNCE_MS + 1)
        input.send("abcd")
        advanceTimeBy(100)
        input.send("abc")
        advanceTimeBy(SEARCH_DEBOUNCE_MS + 1)

        assertEquals(listOf("abc"), output)
    }

    @Test
    fun `пауза ввода - полсекунды`() {
        assertEquals(500L, SEARCH_DEBOUNCE_MS)
    }
}
