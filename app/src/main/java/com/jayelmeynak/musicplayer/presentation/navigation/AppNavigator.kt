package com.jayelmeynak.musicplayer.presentation.navigation

import androidx.navigation3.runtime.NavKey
import com.jayelmeynak.lib.navigation.Navigator

class AppNavigator(private val state: NavigationState) : Navigator {

    override fun navigateTo(destination: NavKey) {
        // Корень стека вкладки — переключить вкладку: корень не переносится в другой стек.
        val tab = state.tabOfRoot(destination)
        if (tab != null) {
            if (state.currentTab != tab) {
                state.currentTab = tab
            }
        } else {
            val stack = state.backStacks[state.currentTab] ?: return
            if (stack.lastOrNull() == destination) return
            // One key lives in one place: equal entries share a content key in NavDisplay, and so
            // their saved state and ViewModelStore. Moving the key between stacks keeps that state,
            // since toEntries decorates all stacks as one list.
            state.backStacks.values.forEach { it.removeAll { key -> key == destination } }
            stack.add(destination)
        }
    }

    /**
     * Переключает на вкладку [tab] и сбрасывает её стек до корня — тап по пункту меню. Операция
     * оболочки: фичам она недоступна, у [Navigator] её нет.
     */
    fun navigateToRoot(tab: AppTab) {
        if (state.currentTab != tab) {
            state.currentTab = tab
        }

        val root = state.rootOf(tab)
        val stack = state.backStacks[tab]
        if (stack != null && (stack.size != 1 || stack.first() != root)) {
            stack.clear()
            stack.add(root)
        }
    }

    override fun goBack() {
        val currentStack = state.backStacks[state.currentTab] ?: return
        when {
            currentStack.size > 1 -> {
                currentStack.removeLastOrNull()
            }

            currentStack.size == 1 -> {
                if (state.currentTab != state.startTab) {
                    state.currentTab = state.startTab
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
