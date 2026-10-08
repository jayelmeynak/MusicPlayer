package com.jayelmeynak.feature.player.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.jayelmeynak.feature.player.api.PlayerDestination
import com.jayelmeynak.feature.player.impl.presentation.PlayerRoute
import com.jayelmeynak.lib.navigation.EntryInstaller
import com.jayelmeynak.lib.navigation.Navigator
import javax.inject.Inject

/** Registers the player screen. Public only for the binding in `:di`. */
public class PlayerEntryInstaller @Inject internal constructor() : EntryInstaller {

    override fun EntryProviderScope<NavKey>.install(navigator: Navigator) {
        entry<PlayerDestination> { PlayerRoute(navigator) }
    }
}
