package com.jayelmeynak.lib.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey

/**
 * Регистрирует записи фичи в общем `entryProvider` приложения, чтобы фича не зависела от `app`.
 */
public fun interface EntryInstaller {
    public fun EntryProviderScope<NavKey>.install(navigator: Navigator)
}
