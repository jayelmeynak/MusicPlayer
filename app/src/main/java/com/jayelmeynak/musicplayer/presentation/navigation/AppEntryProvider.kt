package com.jayelmeynak.musicplayer.presentation.navigation

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import com.jayelmeynak.lib.navigation.EntryInstaller
import com.jayelmeynak.lib.navigation.Navigator

/**
 * Провайдер экранов оболочки: только из установщиков фич, своих `entry<>` у `app` нет. Одна точка
 * сборки для оболочки и теста графа приложения.
 *
 * @throws IllegalArgumentException если два установщика регистрируют один класс ключа (проверка
 *   Navigation3, сообщение называет класс).
 */
fun appEntryProvider(
    installers: Set<EntryInstaller>,
    navigator: Navigator,
): (NavKey) -> NavEntry<NavKey> = entryProvider {
    installers.forEach { installer -> with(installer) { install(navigator) } }
}
