package com.jayelmeynak.lib.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey

/**
 * Registers a feature's entries in the app-wide `entryProvider`, so the feature does not depend on `app`.
 */
public fun interface EntryInstaller {
    public fun EntryProviderScope<NavKey>.install(navigator: Navigator)
}
