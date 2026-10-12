package com.jayelmeynak.musicplayer.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import kotlinx.serialization.json.Json

/**
 * [roots] — корень стека вкладки, если это не сама вкладка (экран фичи, например каталога). Вкладка
 * остаётся [TopLevelDestination]: её хранит и показывает `app`, а корень её стека может быть ключом
 * из `:api` фичи.
 */
@Composable
fun rememberNavigationState(
    startRoute: TopLevelDestination,
    topLevelRoutes: Set<TopLevelDestination>,
    roots: Map<TopLevelDestination, NavKey> = emptyMap(),
): NavigationState {
    val topLevelRoute = rememberSaveable(saver = topLevelRouteSaver(startRoute, topLevelRoutes)) {
        mutableStateOf(startRoute)
    }
    // Keyed by tab, so each stack is saved under its tab rather than its position in the set.
    val backStacks = topLevelRoutes.associateWith { tab -> key(tab) { rememberNavBackStack(roots[tab] ?: tab) } }
    return remember(startRoute, topLevelRoutes, roots) {
        NavigationState(
            startRoute = startRoute,
            topLevelRoute = topLevelRoute,
            backStacks = backStacks,
            roots = roots,
        )
    }
}

/**
 * Saves the current tab with its serializer. A saved value that does not decode to one of
 * [topLevelRoutes] (say, a tab removed in an update) restores [startRoute].
 */
internal fun topLevelRouteSaver(
    startRoute: TopLevelDestination,
    topLevelRoutes: Set<TopLevelDestination>,
): Saver<MutableState<TopLevelDestination>, String> = Saver(
    save = { Json.encodeToString(TopLevelDestination.serializer(), it.value) },
    restore = { saved ->
        val route = runCatching { Json.decodeFromString(TopLevelDestination.serializer(), saved) }.getOrNull()
        mutableStateOf(route?.takeIf { it in topLevelRoutes } ?: startRoute)
    },
)

class NavigationState(
    val startRoute: TopLevelDestination,
    topLevelRoute: MutableState<TopLevelDestination>,
    val backStacks: Map<TopLevelDestination, NavBackStack<NavKey>>,
    private val roots: Map<TopLevelDestination, NavKey> = emptyMap(),
) {
    var topLevelRoute: TopLevelDestination by topLevelRoute

    /** Корень стека вкладки [tab]: ключ из `roots` или сама вкладка. */
    fun rootOf(tab: TopLevelDestination): NavKey = roots[tab] ?: tab

    /** Вкладка, корень стека которой — [key], или `null`, если [key] не корень. */
    fun tabOfRoot(key: NavKey): TopLevelDestination? =
        (key as? TopLevelDestination) ?: roots.entries.firstOrNull { it.value == key }?.key

    val stacksInUse: List<TopLevelDestination>
        get() = if (topLevelRoute == startRoute) {
            listOf(startRoute)
        } else {
            listOf(startRoute, topLevelRoute)
        }
}

/**
 * The entries `NavDisplay` shows: the stacks in [NavigationState.stacksInUse].
 *
 * All stacks are decorated as one list. A key lives in one stack at a time, so a screen moved to
 * another tab's stack stays in that list and is not popped: its ViewModelStore and saved state
 * survive the move. With a decoration per stack the old stack would see the key leave and pop it,
 * clearing the state the new stack has just picked up. Screens of the hidden tab stay decorated, so
 * they live on while the tab is not shown.
 */
@Composable
fun NavigationState.toEntries(
    entryProvider: (NavKey) -> NavEntry<NavKey>,
): List<NavEntry<NavKey>> {
    val tabs = backStacks.keys.toList()
    val stacks = tabs.map { tab -> backStacks.getValue(tab).toList() }
    val keys = stacks.flatten()
    // The shared decoration tells screens apart by key; AppNavigator.navigateTo keeps each key in
    // one stack. A duplicate would show one screen's state in two places, so it is a bug.
    check(keys.size == keys.toSet().size) {
        "A screen key must live in one stack only; change stacks through AppNavigator: $keys"
    }
    val decoratedEntries = rememberDecoratedNavEntries(
        backStack = keys,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider,
    )
    // Decorated entries follow the keys one to one, so each stack is a contiguous slice.
    val entriesByTab = buildMap {
        var from = 0
        tabs.forEachIndexed { index, tab ->
            val until = from + stacks[index].size
            put(tab, decoratedEntries.subList(from, until))
            from = until
        }
    }
    return stacksInUse.flatMap { entriesByTab[it].orEmpty() }
}
