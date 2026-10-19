package com.jayelmeynak.musicplayer.presentation.navigation

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.metadata
import com.jayelmeynak.feature.player.api.HideMiniPlayer
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Мини-плеер решает верхняя показанная запись по своему признаку, а не класс ключа. */
class MiniPlayerVisibilityTest {

    private data object ListKey : NavKey
    private data object FullScreenKey : NavKey

    @Test
    fun `верхняя запись с признаком - мини-плеера нет, какой бы ни был ключ`() {
        val entries = listOf(entry(ListKey), entry(FullScreenKey, hide = true))

        assertFalse(showsMiniPlayer(entries))
    }

    @Test
    fun `признак у записи под верхней - мини-плеер виден`() {
        val entries = listOf(entry(FullScreenKey, hide = true), entry(ListKey))

        assertTrue(showsMiniPlayer(entries))
    }

    @Test
    fun `верхняя запись без признака или с false - мини-плеер виден`() {
        assertTrue(showsMiniPlayer(listOf(entry(ListKey))))
        assertTrue(showsMiniPlayer(listOf(entry(FullScreenKey, hide = false))))
    }

    @Test
    fun `пустой список - мини-плеер виден`() {
        assertTrue(showsMiniPlayer(emptyList()))
    }

    private fun entry(key: NavKey, hide: Boolean? = null): NavEntry<NavKey> = NavEntry(
        key = key,
        metadata = if (hide == null) emptyMap() else metadata { put(HideMiniPlayer, hide) },
    ) {}
}
