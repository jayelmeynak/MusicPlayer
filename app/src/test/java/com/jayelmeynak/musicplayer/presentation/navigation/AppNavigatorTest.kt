package com.jayelmeynak.musicplayer.presentation.navigation

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.jayelmeynak.lib.navigation.Navigator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class AppNavigatorTest {

    /** A screen pushed over a tab root; feature keys live in feature modules. */
    private data class Screen(val id: String) : NavKey

    private lateinit var state: NavigationState
    private lateinit var navigator: AppNavigator

    private val api get() = state.backStacks.getValue(TopLevelDestination.ApiTracks).toList()
    private val local get() = state.backStacks.getValue(TopLevelDestination.DownloadTracks).toList()

    @Before
    fun setUp() {
        state = NavigationState(
            startRoute = TopLevelDestination.ApiTracks,
            topLevelRoute = mutableStateOf<TopLevelDestination>(TopLevelDestination.ApiTracks),
            backStacks = mapOf(
                TopLevelDestination.ApiTracks to NavBackStack(TopLevelDestination.ApiTracks),
                TopLevelDestination.DownloadTracks to NavBackStack(TopLevelDestination.DownloadTracks),
            ),
        )
        navigator = AppNavigator(state)
    }

    @Test
    fun `navigateTo tab switches tab and keeps stacks`() {
        navigator.navigateTo(TopLevelDestination.DownloadTracks)

        assertEquals(TopLevelDestination.DownloadTracks, state.topLevelRoute)
        assertEquals(listOf(TopLevelDestination.ApiTracks), api)
        assertEquals(listOf(TopLevelDestination.DownloadTracks), local)
    }

    @Test
    fun `navigateTo screen pushes onto current tab stack`() {
        navigator.navigateTo(TopLevelDestination.DownloadTracks)
        navigator.navigateTo(Screen("content://1"))

        assertEquals(
            listOf(TopLevelDestination.DownloadTracks, Screen("content://1")),
            local,
        )
        assertEquals(listOf(TopLevelDestination.ApiTracks), api)
    }

    @Test
    fun `switching tabs keeps pushed screen in previous tab stack`() {
        navigator.navigateTo(Screen("1"))
        navigator.navigateToRoot(TopLevelDestination.DownloadTracks)
        navigator.navigateTo(TopLevelDestination.ApiTracks)

        assertEquals(TopLevelDestination.ApiTracks, state.topLevelRoute)
        assertEquals(listOf(TopLevelDestination.ApiTracks, Screen("1")), api)
    }

    @Test
    fun `navigateToRoot resets stack of the tab`() {
        navigator.navigateTo(Screen("1"))
        navigator.navigateTo(Screen("2"))

        navigator.navigateToRoot(TopLevelDestination.ApiTracks)

        assertEquals(TopLevelDestination.ApiTracks, state.topLevelRoute)
        assertEquals(listOf(TopLevelDestination.ApiTracks), api)
    }

    @Test
    fun `navigateToRoot of another tab switches to it and resets its stack`() {
        navigator.navigateTo(TopLevelDestination.DownloadTracks)
        navigator.navigateTo(Screen("content://1"))
        navigator.navigateTo(TopLevelDestination.ApiTracks)

        navigator.navigateToRoot(TopLevelDestination.DownloadTracks)

        assertEquals(TopLevelDestination.DownloadTracks, state.topLevelRoute)
        assertEquals(listOf(TopLevelDestination.DownloadTracks), local)
    }

    @Test
    fun `goBack from depth pops current tab stack`() {
        navigator.navigateTo(Screen("1"))
        navigator.navigateTo(Screen("2"))

        navigator.goBack()

        assertEquals(TopLevelDestination.ApiTracks, state.topLevelRoute)
        assertEquals(listOf(TopLevelDestination.ApiTracks, Screen("1")), api)
    }

    @Test
    fun `goBack from root of non-start tab returns to start tab`() {
        navigator.navigateToRoot(TopLevelDestination.DownloadTracks)

        navigator.goBack()

        assertEquals(TopLevelDestination.ApiTracks, state.topLevelRoute)
        assertEquals(listOf(TopLevelDestination.DownloadTracks), local)
    }

    @Test
    fun `goBack from root of start tab changes nothing`() {
        navigator.goBack()

        assertEquals(TopLevelDestination.ApiTracks, state.topLevelRoute)
        assertEquals(listOf(TopLevelDestination.ApiTracks), api)
        assertEquals(listOf(TopLevelDestination.DownloadTracks), local)
    }

    @Test
    fun `navigateToRoot rejects a non top-level destination and keeps state`() {
        val asInterface: Navigator = navigator

        assertThrows(IllegalArgumentException::class.java) {
            asInterface.navigateToRoot(Screen("1"))
        }
        assertEquals(TopLevelDestination.ApiTracks, state.topLevelRoute)
        assertEquals(listOf(TopLevelDestination.ApiTracks), api)
    }

    @Test
    fun `goBack from depth of non-start tab pops and stays on that tab`() {
        navigator.navigateToRoot(TopLevelDestination.DownloadTracks)
        navigator.navigateTo(Screen("content://1"))

        navigator.goBack()

        assertEquals(TopLevelDestination.DownloadTracks, state.topLevelRoute)
        assertEquals(listOf(TopLevelDestination.DownloadTracks), local)
    }

    @Test
    fun `navigateTo current tab keeps its stack`() {
        navigator.navigateTo(Screen("1"))

        navigator.navigateTo(TopLevelDestination.ApiTracks)

        assertEquals(TopLevelDestination.ApiTracks, state.topLevelRoute)
        assertEquals(listOf(TopLevelDestination.ApiTracks, Screen("1")), api)
    }

    @Test
    fun `navigateTo the screen already on top does not push a duplicate`() {
        navigator.navigateTo(Screen("1"))

        navigator.navigateTo(Screen("1"))

        assertEquals(listOf(TopLevelDestination.ApiTracks, Screen("1")), api)
    }

    @Test
    fun `navigateTo a screen open in another tab moves it to the current tab`() {
        navigator.navigateTo(Screen("1"))
        navigator.navigateTo(TopLevelDestination.DownloadTracks)

        navigator.navigateTo(Screen("1"))

        assertEquals(listOf(TopLevelDestination.DownloadTracks, Screen("1")), local)
        assertEquals(listOf(TopLevelDestination.ApiTracks), api)
    }

    @Test
    fun `navigateTo a screen deeper in the current stack keeps one copy on top`() {
        navigator.navigateTo(Screen("1"))
        navigator.navigateTo(Screen("2"))

        navigator.navigateTo(Screen("1"))

        assertEquals(listOf(TopLevelDestination.ApiTracks, Screen("2"), Screen("1")), api)
    }

    @Test
    fun `navigateToRoot resets a stack whose top is the root but has screens below`() {
        // Not reachable through the navigator; the reset must not rely on the top of the stack alone.
        navigator.navigateTo(Screen("1"))
        state.backStacks.getValue(TopLevelDestination.ApiTracks).add(TopLevelDestination.ApiTracks)

        navigator.navigateToRoot(TopLevelDestination.ApiTracks)

        assertEquals(listOf(TopLevelDestination.ApiTracks), api)
    }
}
