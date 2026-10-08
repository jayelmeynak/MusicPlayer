package com.jayelmeynak.lib.navigation

import androidx.navigation3.runtime.NavKey

/**
 * Moves between screens by [NavKey]. Implemented by `app`; features get it through [EntryInstaller].
 */
public interface Navigator {

    /** Switches to a top-level destination or pushes [destination] onto the current back stack. */
    public fun navigateTo(destination: NavKey)

    /**
     * Switches to the top-level [destination] and resets its back stack to the root.
     *
     * For the host shell (`app`, e.g. a tab click); features must not call it.
     *
     * @throws IllegalArgumentException if [destination] is not a root of one of the host's tabs.
     */
    public fun navigateToRoot(destination: NavKey)

    /** Pops the current back stack. */
    public fun goBack()
}
