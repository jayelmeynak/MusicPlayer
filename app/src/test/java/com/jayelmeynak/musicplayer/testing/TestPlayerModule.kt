package com.jayelmeynak.musicplayer.testing

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jayelmeynak.feature.player.api.PlaybackController
import com.jayelmeynak.feature.player.api.PlaybackState
import com.jayelmeynak.feature.player.api.PlayerDestination
import com.jayelmeynak.feature.player.api.testing.FakePlaybackController
import com.jayelmeynak.feature.player.di.PlayerModule
import com.jayelmeynak.lib.navigation.EntryInstaller
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import dagger.multibindings.IntoSet
import javax.inject.Singleton

/**
 * Оболочка приложения без сервиса плеера: [FakePlaybackController] вместо настоящего контроллера
 * и заглушка экрана плеера (настоящий экран — в `:impl`, `app` его не видит; он покрыт своими тестами).
 */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [PlayerModule::class])
object TestPlayerModule {

    @Provides
    @Singleton
    fun provideFakePlaybackController(): FakePlaybackController = FakePlaybackController()

    @Provides
    fun providePlaybackController(fake: FakePlaybackController): PlaybackController = fake

    @Provides
    @IntoSet
    fun providePlayerStubInstaller(controller: PlaybackController): EntryInstaller = EntryInstaller {
        entry<PlayerDestination> {
            val state by controller.state.collectAsStateWithLifecycle()
            val title = (state as? PlaybackState.Active)?.current?.title.orEmpty()
            Text(text = "$PLAYER_STUB_TEXT$title", modifier = Modifier.testTag(PLAYER_STUB_TAG))
        }
    }

    const val PLAYER_STUB_TAG = "player_stub"
    const val PLAYER_STUB_TEXT = "Экран плеера: "
}
