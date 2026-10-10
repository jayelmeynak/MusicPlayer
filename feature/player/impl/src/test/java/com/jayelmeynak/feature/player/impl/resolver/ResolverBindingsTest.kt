package com.jayelmeynak.feature.player.impl.resolver

import com.jayelmeynak.feature.player.api.TrackSource
import com.jayelmeynak.feature.player.api.TrackUriResolver
import com.jayelmeynak.feature.player.impl.di.PlayerImplModule
import com.jayelmeynak.feature.player.impl.service.resolversBySource
import dagger.multibindings.IntoSet
import org.junit.Assert.assertEquals
import org.junit.Test

class ResolverBindingsTest {

    private val boundResolverTypes = PlayerImplModule::class.java.declaredMethods
        .filter { it.isAnnotationPresent(IntoSet::class.java) && it.returnType == TrackUriResolver::class.java }
        .map { it.parameterTypes.single() }

    @Test
    fun `модуль плеера привязывает в набор резолверы Deezer и локальных треков`() {
        val expected = setOf(DeezerPreviewUriResolver::class.java, LocalContentUriResolver::class.java)

        assertEquals(expected, boundResolverTypes.toSet())
        assertEquals(expected.size, boundResolverTypes.size)
    }

    @Test
    fun `привязанные резолверы покрывают все источники без дублей`() {
        val bound = setOf(DeezerPreviewUriResolver(FakeRemoteTrackDataSource()), LocalContentUriResolver())

        assertEquals(TrackSource.entries.toSet(), resolversBySource(bound).keys)
    }
}
