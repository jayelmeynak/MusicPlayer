package com.jayelmeynak.musicplayer.presentation.navigation

import androidx.navigation3.runtime.NavKey
import com.jayelmeynak.lib.navigation.EntryInstaller
import com.jayelmeynak.lib.navigation.testing.FakeNavigator
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class AppEntryProviderTest {

    private data object DuplicatedKey : NavKey
    private data object OtherKey : NavKey

    @Test
    fun `две регистрации одного класса ключа - исключение с именем ключа`() {
        val first = EntryInstaller { entry<DuplicatedKey> {} }
        val second = EntryInstaller { entry<DuplicatedKey> {} }

        val error = assertThrows(IllegalArgumentException::class.java) {
            appEntryProvider(setOf(first, second), FakeNavigator())
        }

        assertTrue(error.message.orEmpty(), error.message.orEmpty().contains("DuplicatedKey"))
    }

    @Test
    fun `набор без дублей разрешает ключи всех установщиков`() {
        val installers = setOf(
            EntryInstaller { entry<DuplicatedKey> {} },
            EntryInstaller { entry<OtherKey> {} },
        )

        val provider = appEntryProvider(installers, FakeNavigator())

        assertNotNull(provider(DuplicatedKey))
        assertNotNull(provider(OtherKey))
    }
}
