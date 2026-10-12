package com.jayelmeynak.musicplayer.presentation.navigation

import androidx.navigation3.runtime.NavKey
import com.jayelmeynak.lib.navigation.Navigator

class AppNavigator(private val state: NavigationState) : Navigator {

    override fun navigateTo(destination: NavKey) {
        // Вкладка или корень её стека — переключить вкладку: корень не переносится в другой стек.
        val tab = state.tabOfRoot(destination)
        if (tab != null) {
            if (state.topLevelRoute != tab) {
                state.topLevelRoute = tab
            }
        } else {
            val stack = state.backStacks[state.topLevelRoute] ?: return
            if (stack.lastOrNull() == destination) return
            // One key lives in one place: equal entries share a content key in NavDisplay, and so
            // their saved state and ViewModelStore. Moving the key between stacks keeps that state,
            // since toEntries decorates all stacks as one list.
            state.backStacks.values.forEach { it.removeAll { key -> key == destination } }
            stack.add(destination)
        }
    }

    override fun navigateToRoot(destination: NavKey) {
        // Вкладка или корень её стека (ключ фичи из roots), как в navigateTo и remove.
        val tab = state.tabOfRoot(destination)
        require(tab != null) { "Only a top-level destination or its root can be a root: $destination" }
        navigateToRoot(tab)
    }

    fun navigateToRoot(destination: TopLevelDestination) {
        if (state.topLevelRoute != destination) {
            state.topLevelRoute = destination
        }

        val root = state.rootOf(destination)
        val stack = state.backStacks[destination]
        if (stack != null && (stack.size != 1 || stack.first() != root)) {
            stack.clear()
            stack.add(root)
        }
    }

    override fun goBack() {
        val currentStack = state.backStacks[state.topLevelRoute] ?: return
        when {
            currentStack.size > 1 -> {
                currentStack.removeLastOrNull()
            }

            currentStack.size == 1 -> {
                if (state.topLevelRoute != state.startRoute) {
                    state.topLevelRoute = state.startRoute
                }
            }
        }
    }

    override fun remove(destination: NavKey) {
        // Корень вкладки не убирается: стек без корня NavDisplay не покажет.
        if (state.tabOfRoot(destination) != null) return
        state.backStacks.values.forEach { stack -> stack.removeAll { it == destination } }
    }
}
