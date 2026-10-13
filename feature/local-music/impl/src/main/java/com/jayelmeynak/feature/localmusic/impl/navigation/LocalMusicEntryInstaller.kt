package com.jayelmeynak.feature.localmusic.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.jayelmeynak.feature.localmusic.api.LocalMusicDestination
import com.jayelmeynak.feature.localmusic.impl.presentation.LocalTracksRoute
import com.jayelmeynak.lib.navigation.EntryInstaller
import com.jayelmeynak.lib.navigation.Navigator
import javax.inject.Inject

/** Регистрирует экран вкладки «Local». `public` только ради привязки в `:di`. */
public class LocalMusicEntryInstaller @Inject internal constructor() : EntryInstaller {

    override fun EntryProviderScope<NavKey>.install(navigator: Navigator) {
        entry<LocalMusicDestination> { LocalTracksRoute(navigator) }
    }
}
