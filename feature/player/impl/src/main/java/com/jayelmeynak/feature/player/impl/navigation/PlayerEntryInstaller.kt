package com.jayelmeynak.feature.player.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.metadata
import com.jayelmeynak.feature.player.api.HideMiniPlayer
import com.jayelmeynak.feature.player.api.PlayerDestination
import com.jayelmeynak.feature.player.impl.presentation.PlayerRoute
import com.jayelmeynak.lib.navigation.EntryInstaller
import com.jayelmeynak.lib.navigation.Navigator
import javax.inject.Inject

/** Регистрирует экран плеера. Публичный только ради привязки в `:di`. */
public class PlayerEntryInstaller @Inject internal constructor() : EntryInstaller {

    override fun EntryProviderScope<NavKey>.install(navigator: Navigator) {
        // Экран плеера сам показывает трек и управление: мини-плеер над ним не нужен.
        entry<PlayerDestination>(metadata = metadata { put(HideMiniPlayer, true) }) {
            PlayerRoute(navigator)
        }
    }
}
