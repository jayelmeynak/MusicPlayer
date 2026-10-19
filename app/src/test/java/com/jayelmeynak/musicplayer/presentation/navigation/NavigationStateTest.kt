package com.jayelmeynak.musicplayer.presentation.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.jayelmeynak.feature.catalog.api.ChartDestination
import com.jayelmeynak.feature.localmusic.api.LocalMusicDestination
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

/** Экран поверх корня вкладки; сохраняется вместе с back stack, поэтому сериализуемый. */
@Serializable
data class ProbeScreen(val id: String) : NavKey

/** Запоминает, был ли очищен его ViewModelStore. */
class ProbeViewModel : ViewModel() {
    var cleared = false
        private set

    override fun onCleared() {
        cleared = true
    }
}

// Robolectric: оболочке нужна настоящая ComponentActivity с ViewModelStore и сохранённым состоянием.
@RunWith(RobolectricTestRunner::class)
class NavigationStateTest {

    @get:Rule
    val compose = createComposeRule()

    private lateinit var state: NavigationState
    private lateinit var navigator: AppNavigator

    /** Все разные экземпляры ViewModel пробного экрана, в порядке создания. */
    private val probes = mutableListOf<ProbeViewModel>()

    /** Все разные saveable-значения пробного экрана, в порядке создания. */
    private val savedValues = mutableListOf<String>()

    private val api get() = state.backStacks.getValue(AppTab.Catalog).toList()
    private val local get() = state.backStacks.getValue(AppTab.LocalMusic).toList()

    /** Оболочка приложения без экранов фич: то же состояние, навигатор и декорация. */
    @Composable
    private fun Shell() {
        val navigationState = rememberNavigationState(
            startTab = AppTab.Catalog,
            tabs = AppTab.entries,
        )
        val appNavigator = remember(navigationState) { AppNavigator(navigationState) }
        state = navigationState
        navigator = appNavigator

        val entries = navigationState.toEntries(
            entryProvider {
                entry<ChartDestination> { Text("api root") }
                entry<LocalMusicDestination> { Text("local root") }
                entry<ProbeScreen> { key ->
                    val probe = viewModel { ProbeViewModel() }
                    val saved = rememberSaveable { UUID.randomUUID().toString() }
                    SideEffect {
                        if (probes.none { it === probe }) probes += probe
                        if (saved !in savedValues) savedValues += saved
                    }
                    Text("screen ${key.id}")
                }
            }
        )
        NavDisplay(entries = entries, onBack = { appNavigator.goBack() })
    }

    private fun navigate(action: AppNavigator.() -> Unit) {
        compose.runOnIdle { navigator.action() }
        compose.waitForIdle()
    }

    @Test
    fun `screen moved to another tab stack keeps its view model and saved state`() {
        compose.setContent { Shell() }
        navigate { navigateTo(ProbeScreen("player")) }
        navigate { navigateTo(LocalMusicDestination) }

        navigate { navigateTo(ProbeScreen("player")) }

        compose.onNodeWithText("screen player").assertExists()
        assertEquals(listOf(LocalMusicDestination, ProbeScreen("player")), local)
        assertEquals(listOf<NavKey>(ChartDestination), api)
        assertEquals(1, probes.size)
        assertFalse(probes.single().cleared)
        assertEquals(1, savedValues.size)
    }

    @Test
    fun `popping a moved screen clears its view model`() {
        compose.setContent { Shell() }
        navigate { navigateTo(ProbeScreen("player")) }
        navigate { navigateTo(LocalMusicDestination) }
        navigate { navigateTo(ProbeScreen("player")) }

        navigate { goBack() }

        compose.onNodeWithText("local root").assertExists()
        assertEquals(1, probes.size)
        assertTrue(probes.single().cleared)
    }

    @Test
    fun `resetting the current tab clears the view model of the screen on top`() {
        compose.setContent { Shell() }
        navigate { navigateTo(ProbeScreen("player")) }

        navigate { navigateToRoot(AppTab.Catalog) }

        compose.onNodeWithText("api root").assertExists()
        assertEquals(1, probes.size)
        assertTrue(probes.single().cleared)
    }

    @Test
    fun `resetting the hidden tab clears its screen and a new open starts fresh`() {
        compose.setContent { Shell() }
        navigate { navigateToRoot(AppTab.LocalMusic) }
        navigate { navigateTo(ProbeScreen("player")) }
        navigate { navigateTo(ChartDestination) }

        navigate { navigateToRoot(AppTab.LocalMusic) }

        compose.onNodeWithText("local root").assertExists()
        assertEquals(1, probes.size)
        assertTrue(probes.single().cleared)

        navigate { navigateTo(ProbeScreen("player")) }

        assertEquals(2, probes.size)
        assertFalse(probes.last().cleared)
        assertEquals(2, savedValues.size)
    }

    @Test
    fun `a key in two stacks fails fast`() {
        compose.setContent { Shell() }
        navigate { navigateTo(ProbeScreen("player")) }

        val error = runCatching {
            compose.runOnIdle {
                state.backStacks.getValue(AppTab.LocalMusic).add(ProbeScreen("player"))
            }
            compose.waitForIdle()
        }.exceptionOrNull()

        assertTrue("expected IllegalStateException, got $error", error is IllegalStateException)
    }

    @Test
    fun `screen in the hidden tab stack keeps its view model across tab switches`() {
        compose.setContent { Shell() }
        navigate { navigateTo(ProbeScreen("player")) }

        navigate { navigateTo(LocalMusicDestination) }
        navigate { navigateTo(ChartDestination) }

        compose.onNodeWithText("screen player").assertExists()
        assertEquals(1, probes.size)
        assertFalse(probes.single().cleared)
    }

    @Test
    fun `current tab and both stacks survive recreation`() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { Shell() }
        navigate { navigateTo(ProbeScreen("in-remote-tab")) }
        navigate { navigateToRoot(AppTab.LocalMusic) }
        navigate { navigateTo(ProbeScreen("in-local-tab")) }

        restoration.emulateSavedInstanceStateRestore()

        compose.waitForIdle()
        assertEquals(AppTab.LocalMusic, state.currentTab)
        assertEquals(listOf(LocalMusicDestination, ProbeScreen("in-local-tab")), local)
        assertEquals(listOf(ChartDestination, ProbeScreen("in-remote-tab")), api)
        compose.onNodeWithText("screen in-local-tab").assertExists()
    }

    @Test
    fun `start tab survives recreation`() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { Shell() }
        navigate { navigateTo(ProbeScreen("in-remote-tab")) }

        restoration.emulateSavedInstanceStateRestore()

        compose.waitForIdle()
        assertEquals(AppTab.Catalog, state.currentTab)
        assertEquals(listOf(ChartDestination, ProbeScreen("in-remote-tab")), api)
        compose.onNodeWithText("screen in-remote-tab").assertExists()
    }

    @Test
    fun `saved tab round-trips through the saver`() {
        val saver = currentTabSaver(startTab = AppTab.Catalog, tabs = AppTab.entries)

        val saved = with(saver) { SaverScope { true }.save(mutableStateOf(AppTab.LocalMusic)) }

        assertEquals(AppTab.LocalMusic, saver.restore(checkNotNull(saved))?.value)
    }

    @Test
    fun `saved tab that is no longer a tab restores the start tab`() {
        val saver = currentTabSaver(startTab = AppTab.Catalog, tabs = listOf(AppTab.Catalog))
        val removedTab = with(currentTabSaver(AppTab.Catalog, AppTab.entries)) {
            SaverScope { true }.save(mutableStateOf(AppTab.LocalMusic))
        }

        assertEquals(AppTab.Catalog, saver.restore(checkNotNull(removedTab))?.value)
        assertEquals(AppTab.Catalog, saver.restore("not a saved tab")?.value)
        // Вкладка прежнего формата (JSON ключа навигации) тоже даёт стартовую.
        assertEquals(
            AppTab.Catalog,
            saver.restore("""{"type":"com.jayelmeynak.musicplayer.presentation.navigation.LocalMusicDestination"}""")?.value,
        )
    }

    @Test
    fun `стек каждой вкладки начинается с её корня`() {
        compose.setContent { Shell() }
        compose.waitForIdle()

        assertEquals(listOf<NavKey>(ChartDestination), api)
        assertEquals(listOf<NavKey>(LocalMusicDestination), local)
        compose.onNodeWithText("api root").assertExists()
    }
}
