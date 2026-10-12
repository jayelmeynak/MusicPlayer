package com.jayelmeynak.feature.player.impl.resolver

import com.jayelmeynak.feature.player.api.TrackUriResolver
import com.jayelmeynak.feature.player.impl.di.PlayerImplModule
import dagger.multibindings.IntoSet
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Привязки резолверов в модуле плеера. Что набор всего приложения покрывает все источники без
 * дублей, проверяет `TrackUriResolverGraphTest` в `app`: резолвер Deezer привязывает каталог.
 */
class ResolverBindingsTest {

    private val boundResolverTypes = PlayerImplModule::class.java.declaredMethods
        .filter { it.isAnnotationPresent(IntoSet::class.java) && it.returnType == TrackUriResolver::class.java }
        .map { it.parameterTypes.single() }

    @Test
    fun `модуль плеера привязывает в набор только резолвер локальных треков`() {
        assertEquals(listOf(LocalContentUriResolver::class.java), boundResolverTypes)
    }
}
