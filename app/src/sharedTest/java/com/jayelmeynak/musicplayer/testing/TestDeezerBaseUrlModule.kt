package com.jayelmeynak.musicplayer.testing

import com.jayelmeynak.lib.network.di.DeezerBaseUrl
import com.jayelmeynak.lib.network.di.DeezerBaseUrlModule
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn

/**
 * Deezer в тестах — локальный [FakeDeezerServer]; сервер стартует тестовое правило до запуска Activity.
 * Один файл на оба слоя: `sharedTest` компилируется и обрабатывается Hilt в `test` и в `androidTest`.
 */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [DeezerBaseUrlModule::class])
object TestDeezerBaseUrlModule {

    @Provides
    @DeezerBaseUrl
    fun provideDeezerBaseUrl(): String = FakeDeezerServer.baseUrl
}
