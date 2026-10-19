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

/**
 * Состояние оболочки: текущая вкладка из [tabs] (стартовая — [startTab]) и стек каждой вкладки,
 * начинающийся с её корня [AppTab.root].
 */
@Composable
fun rememberNavigationState(
    startTab: AppTab,
    tabs: List<AppTab>,
): NavigationState {
    val currentTab = rememberSaveable(saver = currentTabSaver(startTab, tabs)) {
        mutableStateOf(startTab)
    }
    // Keyed by tab, so each stack is saved under its tab rather than its position in the list.
    val backStacks = tabs.associateWith { tab -> key(tab.name) { rememberNavBackStack(tab.root) } }
    return remember(startTab, tabs) {
        NavigationState(
            startTab = startTab,
            currentTab = currentTab,
            backStacks = backStacks,
        )
    }
}

/**
 * Сохраняет текущую вкладку по имени. Значение, которое не является именем одной из [tabs]
 * (вкладку убрали в обновлении, прежний формат), восстанавливает [startTab].
 */
internal fun currentTabSaver(
    startTab: AppTab,
    tabs: List<AppTab>,
): Saver<MutableState<AppTab>, String> = Saver(
    save = { it.value.name },
    restore = { saved -> mutableStateOf(tabs.firstOrNull { it.name == saved } ?: startTab) },
)

class NavigationState(
    val startTab: AppTab,
    currentTab: MutableState<AppTab>,
    val backStacks: Map<AppTab, NavBackStack<NavKey>>,
) {
    init {
        val roots = backStacks.keys.map { it.root }
        require(roots.size == roots.toSet().size) { "Корни вкладок должны быть различны: $roots" }
    }

    var currentTab: AppTab by currentTab

    /** Корень стека вкладки [tab]. */
    fun rootOf(tab: AppTab): NavKey = tab.root

    /** Вкладка, корень стека которой — [key], или `null`, если [key] не корень. */
    fun tabOfRoot(key: NavKey): AppTab? = backStacks.keys.firstOrNull { it.root == key }

    val stacksInUse: List<AppTab>
        get() = if (currentTab == startTab) {
            listOf(startTab)
        } else {
            listOf(startTab, currentTab)
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
