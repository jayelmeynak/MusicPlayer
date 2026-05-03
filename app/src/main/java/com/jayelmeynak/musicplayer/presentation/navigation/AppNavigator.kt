package com.jayelmeynak.musicplayer.presentation.navigation

import androidx.navigation3.runtime.NavKey

class AppNavigator(private val state: NavigationState) {

    fun navigateTo(destination: NavKey) {
        if (destination is TopLevelDestination) {
            if (state.topLevelRoute != destination) {
                state.topLevelRoute = destination
            }
        } else {
            state.backStacks[state.currentTopLevel]?.add(destination)
        }
    }

    fun navigateToRoot(destination: TopLevelDestination) {
        if (state.topLevelRoute != destination) {
            state.topLevelRoute = destination
        }

        val stack = state.backStacks[destination]
        if (stack != null && stack.lastOrNull() != destination) {
            stack.clear()
            stack.add(destination)
        }
    }

    fun goBack() {
        val currentStack = state.backStacks[state.currentTopLevel] ?: return
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
}