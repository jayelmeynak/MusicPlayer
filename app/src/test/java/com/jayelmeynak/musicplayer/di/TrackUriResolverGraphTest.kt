package com.jayelmeynak.musicplayer.di

import com.jayelmeynak.feature.player.api.TrackSource
import com.jayelmeynak.feature.player.api.TrackUriResolver
import com.jayelmeynak.musicplayer.testing.FakeDeezerServerRule
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.inject.Inject

/**
 * Резолверы источников привязывают разные модули (локальный — local-music, Deezer — каталог); плеер
 * падает при создании, если источника нет или он привязан дважды. Проверяется граф Hilt всего
 * приложения. Robolectric — ради Hilt-компонента приложения.
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class TrackUriResolverGraphTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    // Резолвер Deezer тянет сетевой клиент, а адрес Deezer в тестах — фейковый сервер.
    @get:Rule(order = 1)
    val serverRule = FakeDeezerServerRule()

    @Inject
    lateinit var resolvers: Set<@JvmSuppressWildcards TrackUriResolver>

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun `резолверы приложения покрывают все источники, по одному на источник`() {
        val sources = resolvers.map { it.source }

        assertEquals(TrackSource.entries.toSet(), sources.toSet())
        assertEquals(TrackSource.entries.size, sources.size)
    }
}
