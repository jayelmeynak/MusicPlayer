package com.jayelmeynak.feature.player.impl.navigation

import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.get
import com.jayelmeynak.feature.player.api.HideMiniPlayer
import com.jayelmeynak.feature.player.api.PlayerDestination
import com.jayelmeynak.lib.navigation.testing.FakeNavigator
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerEntryInstallerTest {

    @Test
    fun `запись экрана плеера объявляет, что мини-плеер на ней не показывается`() {
        val installer = PlayerEntryInstaller()
        val provider = entryProvider<NavKey> { with(installer) { install(FakeNavigator()) } }

        val entry = provider(PlayerDestination)

        assertEquals(true, entry.metadata[HideMiniPlayer])
    }
}
