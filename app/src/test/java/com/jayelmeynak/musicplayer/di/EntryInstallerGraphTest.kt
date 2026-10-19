package com.jayelmeynak.musicplayer.di

import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.get
import com.jayelmeynak.feature.player.api.HideMiniPlayer
import com.jayelmeynak.feature.player.api.PlayerDestination
import com.jayelmeynak.lib.navigation.EntryInstaller
import com.jayelmeynak.lib.navigation.testing.FakeNavigator
import com.jayelmeynak.musicplayer.presentation.navigation.AppTab
import com.jayelmeynak.musicplayer.presentation.navigation.appEntryProvider
import com.jayelmeynak.musicplayer.testing.FakeDeezerServerRule
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.inject.Inject

/**
 * Экраны регистрируют разные модули `:di`; дубль ключа или корень вкладки без экрана Navigation3
 * ловит только при показе. Проверяется граф Hilt всего приложения тем же кодом, что строит
 * провайдер оболочки. Экран плеера здесь — заглушка `TestPlayerModule` (подменяет `PlayerModule`).
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class EntryInstallerGraphTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    // Экран каталога тянет сетевой клиент, а адрес Deezer в тестах — фейковый сервер.
    @get:Rule(order = 1)
    val serverRule = FakeDeezerServerRule()

    @Inject
    lateinit var installers: Set<@JvmSuppressWildcards EntryInstaller>

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun `экраны приложения без дублей покрывают корни всех вкладок и плеер`() {
        val provider = appEntryProvider(installers, FakeNavigator())
        val keys: List<NavKey> = AppTab.entries.map { it.root } + PlayerDestination

        // Неизвестный ключ — исключение «Unknown screen», ключ записи по умолчанию — его строка.
        keys.forEach { key -> assertNotNull(provider(key)) }
    }

    @Test
    fun `экраны корней вкладок не прячут мини-плеер`() {
        val provider = appEntryProvider(installers, FakeNavigator())

        AppTab.entries.forEach { tab -> assertNull(tab.name, provider(tab.root).metadata[HideMiniPlayer]) }
    }
}
