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
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

/** A screen pushed over a tab root; saved with the back stack, so it is serializable. */
@Serializable
data class ProbeScreen(val id: String) : NavKey

/** Records whether its ViewModelStore was cleared. */
class ProbeViewModel : ViewModel() {
    var cleared = false
        private set

    override fun onCleared() {
        cleared = true
    }
}

// Robolectric: the shell needs a real ComponentActivity with its ViewModelStore and saved state.
@RunWith(RobolectricTestRunner::class)
class NavigationStateTest {

    @get:Rule
    val compose = createComposeRule()

    private lateinit var state: NavigationState
    private lateinit var navigator: AppNavigator

    /** Every distinct ViewModel instance the probe screen got, in creation order. */
    private val probes = mutableListOf<ProbeViewModel>()

    /** Every distinct saveable value the probe screen got, in creation order. */
    private val savedValues = mutableListOf<String>()

    private val api get() = state.backStacks.getValue(TopLevelDestination.ApiTracks).toList()
    private val local get() = state.backStacks.getValue(TopLevelDestination.DownloadTracks).toList()

    /** The app shell without the feature screens: the same state, navigator and decoration. */
    @Composable
    private fun Shell() {
        val navigationState = rememberNavigationState(
            startRoute = TopLevelDestination.ApiTracks,
            topLevelRoutes = setOf(TopLevelDestination.ApiTracks, TopLevelDestination.DownloadTracks),
        )
        val appNavigator = remember(navigationState) { AppNavigator(navigationState) }
        state = navigationState
        navigator = appNavigator

        val entries = navigationState.toEntries(
            entryProvider {
                entry<TopLevelDestination.ApiTracks> { Text("api root") }
                entry<TopLevelDestination.DownloadTracks> { Text("local root") }
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
        navigate { navigateTo(TopLevelDestination.DownloadTracks) }

        navigate { navigateTo(ProbeScreen("player")) }

        compose.onNodeWithText("screen player").assertExists()
        assertEquals(listOf(TopLevelDestination.DownloadTracks, ProbeScreen("player")), local)
        assertEquals(listOf<NavKey>(TopLevelDestination.ApiTracks), api)
        assertEquals(1, probes.size)
        assertFalse(probes.single().cleared)
        assertEquals(1, savedValues.size)
    }

    @Test
    fun `popping a moved screen clears its view model`() {
        compose.setContent { Shell() }
        navigate { navigateTo(ProbeScreen("player")) }
        navigate { navigateTo(TopLevelDestination.DownloadTracks) }
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

        navigate { navigateToRoot(TopLevelDestination.ApiTracks) }

        compose.onNodeWithText("api root").assertExists()
        assertEquals(1, probes.size)
        assertTrue(probes.single().cleared)
    }

    @Test
    fun `resetting the hidden tab clears its screen and a new open starts fresh`() {
        compose.setContent { Shell() }
        navigate { navigateToRoot(TopLevelDestination.DownloadTracks) }
        navigate { navigateTo(ProbeScreen("player")) }
        navigate { navigateTo(TopLevelDestination.ApiTracks) }

        navigate { navigateToRoot(TopLevelDestination.DownloadTracks) }

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
                state.backStacks.getValue(TopLevelDestination.DownloadTracks).add(ProbeScreen("player"))
            }
            compose.waitForIdle()
        }.exceptionOrNull()

        assertTrue("expected IllegalStateException, got $error", error is IllegalStateException)
    }

    @Test
    fun `screen in the hidden tab stack keeps its view model across tab switches`() {
        compose.setContent { Shell() }
        navigate { navigateTo(ProbeScreen("player")) }

        navigate { navigateTo(TopLevelDestination.DownloadTracks) }
        navigate { navigateTo(TopLevelDestination.ApiTracks) }

        compose.onNodeWithText("screen player").assertExists()
        assertEquals(1, probes.size)
        assertFalse(probes.single().cleared)
    }

    @Test
    fun `current tab and both stacks survive recreation`() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { Shell() }
        navigate { navigateTo(ProbeScreen("remote")) }
        navigate { navigateToRoot(TopLevelDestination.DownloadTracks) }
        navigate { navigateTo(ProbeScreen("local")) }

        restoration.emulateSavedInstanceStateRestore()

        compose.waitForIdle()
        assertEquals(TopLevelDestination.DownloadTracks, state.topLevelRoute)
        assertEquals(listOf(TopLevelDestination.DownloadTracks, ProbeScreen("local")), local)
        assertEquals(listOf(TopLevelDestination.ApiTracks, ProbeScreen("remote")), api)
        compose.onNodeWithText("screen local").assertExists()
    }

    @Test
    fun `start tab survives recreation`() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { Shell() }
        navigate { navigateTo(ProbeScreen("remote")) }

        restoration.emulateSavedInstanceStateRestore()

        compose.waitForIdle()
        assertEquals(TopLevelDestination.ApiTracks, state.topLevelRoute)
        assertEquals(listOf(TopLevelDestination.ApiTracks, ProbeScreen("remote")), api)
        compose.onNodeWithText("screen remote").assertExists()
    }

    @Test
    fun `saved tab round-trips through the saver`() {
        val saver = topLevelRouteSaver(
            startRoute = TopLevelDestination.ApiTracks,
            topLevelRoutes = setOf(TopLevelDestination.ApiTracks, TopLevelDestination.DownloadTracks),
        )

        val saved = with(saver) { SaverScope { true }.save(mutableStateOf(TopLevelDestination.DownloadTracks)) }

        assertEquals(TopLevelDestination.DownloadTracks, saver.restore(checkNotNull(saved))?.value)
    }

    @Test
    fun `saved tab that is no longer a tab restores the start tab`() {
        val saver = topLevelRouteSaver(
            startRoute = TopLevelDestination.ApiTracks,
            topLevelRoutes = setOf(TopLevelDestination.ApiTracks),
        )
        val removedTab = with(topLevelRouteSaver(TopLevelDestination.ApiTracks, setOf(TopLevelDestination.DownloadTracks))) {
            SaverScope { true }.save(mutableStateOf(TopLevelDestination.DownloadTracks))
        }

        assertEquals(TopLevelDestination.ApiTracks, saver.restore(checkNotNull(removedTab))?.value)
        assertEquals(TopLevelDestination.ApiTracks, saver.restore("not a saved tab")?.value)
    }
}
