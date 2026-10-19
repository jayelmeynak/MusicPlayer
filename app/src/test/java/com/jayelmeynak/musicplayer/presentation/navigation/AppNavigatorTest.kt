package com.jayelmeynak.musicplayer.presentation.navigation

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.jayelmeynak.feature.catalog.api.ChartDestination
import com.jayelmeynak.feature.localmusic.api.LocalMusicDestination
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class AppNavigatorTest {

    /** Экран поверх корня вкладки; корни вкладок — ключи фич (`ChartDestination`, `LocalMusicDestination`). */
    private data class Screen(val id: String) : NavKey

    private lateinit var state: NavigationState
    private lateinit var navigator: AppNavigator

    private val api get() = state.backStacks.getValue(AppTab.Catalog).toList()
    private val local get() = state.backStacks.getValue(AppTab.LocalMusic).toList()

    @Before
    fun setUp() {
        state = NavigationState(
            startTab = AppTab.Catalog,
            currentTab = mutableStateOf(AppTab.Catalog),
            backStacks = mapOf(
                AppTab.Catalog to NavBackStack(ChartDestination),
                AppTab.LocalMusic to NavBackStack(LocalMusicDestination),
            ),
        )
        navigator = AppNavigator(state)
    }

    @Test
    fun `remove убирает экран из стека, даже если поверх открыт другой`() {
        navigator.navigateTo(Screen("player"))
        navigator.navigateTo(Screen("other"))

        navigator.remove(Screen("player"))

        assertEquals(listOf(ChartDestination, Screen("other")), api)
        assertEquals(AppTab.Catalog, state.currentTab)
    }

    @Test
    fun `remove ищет ключ во всех стеках и не меняет вкладку`() {
        navigator.navigateTo(Screen("player"))
        navigator.navigateTo(LocalMusicDestination)

        navigator.remove(Screen("player"))

        assertEquals(listOf(ChartDestination), api)
        assertEquals(AppTab.LocalMusic, state.currentTab)
    }

    @Test
    fun `remove корня вкладки и отсутствующего ключа ничего не делает`() {
        navigator.navigateTo(Screen("1"))

        navigator.remove(ChartDestination)
        navigator.remove(Screen("missing"))

        assertEquals(listOf(ChartDestination, Screen("1")), api)
        assertEquals(listOf(LocalMusicDestination), local)
    }

    @Test
    fun `navigateTo tab switches tab and keeps stacks`() {
        navigator.navigateTo(LocalMusicDestination)

        assertEquals(AppTab.LocalMusic, state.currentTab)
        assertEquals(listOf(ChartDestination), api)
        assertEquals(listOf(LocalMusicDestination), local)
    }

    @Test
    fun `navigateTo screen pushes onto current tab stack`() {
        navigator.navigateTo(LocalMusicDestination)
        navigator.navigateTo(Screen("content://1"))

        assertEquals(
            listOf(LocalMusicDestination, Screen("content://1")),
            local,
        )
        assertEquals(listOf(ChartDestination), api)
    }

    @Test
    fun `switching tabs keeps pushed screen in previous tab stack`() {
        navigator.navigateTo(Screen("1"))
        navigator.navigateToRoot(AppTab.LocalMusic)
        navigator.navigateTo(ChartDestination)

        assertEquals(AppTab.Catalog, state.currentTab)
        assertEquals(listOf(ChartDestination, Screen("1")), api)
    }

    @Test
    fun `navigateToRoot resets stack of the tab`() {
        navigator.navigateTo(Screen("1"))
        navigator.navigateTo(Screen("2"))

        navigator.navigateToRoot(AppTab.Catalog)

        assertEquals(AppTab.Catalog, state.currentTab)
        assertEquals(listOf(ChartDestination), api)
    }

    @Test
    fun `navigateToRoot of another tab switches to it and resets its stack`() {
        navigator.navigateTo(LocalMusicDestination)
        navigator.navigateTo(Screen("content://1"))
        navigator.navigateTo(ChartDestination)

        navigator.navigateToRoot(AppTab.LocalMusic)

        assertEquals(AppTab.LocalMusic, state.currentTab)
        assertEquals(listOf(LocalMusicDestination), local)
    }

    @Test
    fun `goBack from depth pops current tab stack`() {
        navigator.navigateTo(Screen("1"))
        navigator.navigateTo(Screen("2"))

        navigator.goBack()

        assertEquals(AppTab.Catalog, state.currentTab)
        assertEquals(listOf(ChartDestination, Screen("1")), api)
    }

    @Test
    fun `goBack from root of non-start tab returns to start tab`() {
        navigator.navigateToRoot(AppTab.LocalMusic)

        navigator.goBack()

        assertEquals(AppTab.Catalog, state.currentTab)
        assertEquals(listOf(LocalMusicDestination), local)
    }

    @Test
    fun `goBack from root of start tab changes nothing`() {
        navigator.goBack()

        assertEquals(AppTab.Catalog, state.currentTab)
        assertEquals(listOf(ChartDestination), api)
        assertEquals(listOf(LocalMusicDestination), local)
    }

    @Test
    fun `goBack from depth of non-start tab pops and stays on that tab`() {
        navigator.navigateToRoot(AppTab.LocalMusic)
        navigator.navigateTo(Screen("content://1"))

        navigator.goBack()

        assertEquals(AppTab.LocalMusic, state.currentTab)
        assertEquals(listOf(LocalMusicDestination), local)
    }

    @Test
    fun `navigateTo current tab keeps its stack`() {
        navigator.navigateTo(Screen("1"))

        navigator.navigateTo(ChartDestination)

        assertEquals(AppTab.Catalog, state.currentTab)
        assertEquals(listOf(ChartDestination, Screen("1")), api)
    }

    @Test
    fun `navigateTo the screen already on top does not push a duplicate`() {
        navigator.navigateTo(Screen("1"))

        navigator.navigateTo(Screen("1"))

        assertEquals(listOf(ChartDestination, Screen("1")), api)
    }

    @Test
    fun `navigateTo a screen open in another tab moves it to the current tab`() {
        navigator.navigateTo(Screen("1"))
        navigator.navigateTo(LocalMusicDestination)

        navigator.navigateTo(Screen("1"))

        assertEquals(listOf(LocalMusicDestination, Screen("1")), local)
        assertEquals(listOf(ChartDestination), api)
    }

    @Test
    fun `navigateTo a screen deeper in the current stack keeps one copy on top`() {
        navigator.navigateTo(Screen("1"))
        navigator.navigateTo(Screen("2"))

        navigator.navigateTo(Screen("1"))

        assertEquals(listOf(ChartDestination, Screen("2"), Screen("1")), api)
    }

    @Test
    fun `navigateToRoot resets a stack whose top is the root but has screens below`() {
        // Через навигатор не достижимо; сброс не должен опираться только на верх стека.
        navigator.navigateTo(Screen("1"))
        state.backStacks.getValue(AppTab.Catalog).add(ChartDestination)

        navigator.navigateToRoot(AppTab.Catalog)

        assertEquals(listOf(ChartDestination), api)
    }

    @Test
    fun `navigateTo корня вкладки переключает на неё и не переносит корень в текущий стек`() {
        navigator.navigateTo(LocalMusicDestination)

        navigator.navigateTo(ChartDestination)

        assertEquals(AppTab.Catalog, state.currentTab)
        assertEquals(listOf<NavKey>(ChartDestination), api)
        assertEquals(listOf<NavKey>(LocalMusicDestination), local)
    }
}
