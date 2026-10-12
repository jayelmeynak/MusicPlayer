package com.jayelmeynak.feature.catalog.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.jayelmeynak.feature.catalog.api.ChartDestination
import com.jayelmeynak.feature.catalog.impl.presentation.CatalogRoute
import com.jayelmeynak.lib.navigation.EntryInstaller
import com.jayelmeynak.lib.navigation.Navigator
import javax.inject.Inject

/** Регистрирует экран каталога. `public` только ради привязки в `:di`. */
public class CatalogEntryInstaller @Inject internal constructor() : EntryInstaller {

    override fun EntryProviderScope<NavKey>.install(navigator: Navigator) {
        entry<ChartDestination> { CatalogRoute(navigator) }
    }
}
