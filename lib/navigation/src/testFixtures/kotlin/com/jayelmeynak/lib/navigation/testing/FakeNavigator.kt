package com.jayelmeynak.lib.navigation.testing

import androidx.navigation3.runtime.NavKey
import com.jayelmeynak.lib.navigation.Navigator

/** [Navigator] for tests: records where it was asked to go. */
public class FakeNavigator : Navigator {

    /** Keys passed to [navigateTo], in call order. */
    public val destinations: MutableList<NavKey> = mutableListOf()

    /** Keys passed to [navigateToRoot], in call order. */
    public val rootDestinations: MutableList<NavKey> = mutableListOf()

    /** How many times [goBack] was called. */
    public var backCount: Int = 0
        private set

    override fun navigateTo(destination: NavKey) {
        destinations += destination
    }

    override fun navigateToRoot(destination: NavKey) {
        rootDestinations += destination
    }

    override fun goBack() {
        backCount++
    }
}
