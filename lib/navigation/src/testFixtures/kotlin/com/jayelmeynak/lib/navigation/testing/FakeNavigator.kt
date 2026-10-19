package com.jayelmeynak.lib.navigation.testing

import androidx.navigation3.runtime.NavKey
import com.jayelmeynak.lib.navigation.Navigator

/** [Navigator] для тестов: запоминает, куда его просили перейти. */
public class FakeNavigator : Navigator {

    /** Ключи, переданные в [navigateTo], в порядке вызова. */
    public val destinations: MutableList<NavKey> = mutableListOf()

    /** Ключи, переданные в [remove], в порядке вызовов. */
    public val removed: MutableList<NavKey> = mutableListOf()

    /** Сколько раз вызван [goBack]. */
    public var backCount: Int = 0
        private set

    override fun navigateTo(destination: NavKey) {
        destinations += destination
    }

    override fun goBack() {
        backCount++
    }

    override fun remove(destination: NavKey) {
        removed += destination
    }
}
