package com.jayelmeynak.lib.navigation

import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import com.jayelmeynak.lib.navigation.testing.FakeNavigator
import org.junit.Assert.assertEquals
import org.junit.Test

class EntryInstallerTest {

    private data object ListKey : NavKey
    private data class DetailKey(val id: String) : NavKey

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

        assertEquals(listOf<NavKey>(DetailKey("1")), navigator.destinations)
        assertEquals(emptyList<NavKey>(), navigator.rootDestinations)
        assertEquals(0, navigator.backCount)
    }

    private companion object {
        const val ON_CLICK = "onClick"
    }
}
