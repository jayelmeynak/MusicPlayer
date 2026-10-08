package com.jayelmeynak.lib.navigation

import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import org.junit.Assert.assertEquals
import org.junit.Test

class EntryInstallerTest {

    private data object ListKey : NavKey
    private data class DetailKey(val id: String) : NavKey

    private class FakeNavigator : Navigator {
        val calls = mutableListOf<String>()
        override fun navigateTo(destination: NavKey) { calls += "navigateTo($destination)" }
        override fun navigateToRoot(destination: NavKey) { calls += "navigateToRoot($destination)" }
        override fun goBack() { calls += "goBack" }
    }

    @Test
    fun `installer registers entry that navigates through the given navigator`() {
        val navigator = FakeNavigator()
        val installer = EntryInstaller { nav ->
            entry<ListKey>(metadata = { mapOf(ON_CLICK to { nav.navigateTo(DetailKey("1")) }) }) {}
        }

        val provider = entryProvider<NavKey> { with(installer) { install(navigator) } }
        val entry = provider(ListKey)
        @Suppress("UNCHECKED_CAST")
        (entry.metadata.getValue(ON_CLICK) as () -> Unit).invoke()

        assertEquals(listOf("navigateTo(${DetailKey("1")})"), navigator.calls)
    }

    private companion object {
        const val ON_CLICK = "onClick"
    }
}
