package com.jayelmeynak.feature.player.impl.presentation

import androidx.navigation3.runtime.NavKey
import com.jayelmeynak.lib.navigation.Navigator

internal class FakeNavigator : Navigator {
    val destinations = mutableListOf<NavKey>()
    var backCount = 0
    override fun navigateTo(destination: NavKey) { destinations += destination }
    override fun navigateToRoot(destination: NavKey) = error("not used")
    override fun goBack() { backCount++ }
}
