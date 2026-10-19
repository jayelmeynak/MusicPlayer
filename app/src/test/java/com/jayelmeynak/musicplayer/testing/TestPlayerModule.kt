package com.jayelmeynak.musicplayer.testing

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.metadata
import com.jayelmeynak.feature.player.api.HideMiniPlayer
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
import java.util.concurrent.atomic.AtomicInteger
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
        // Как настоящий экран плеера: мини-плеера над ним нет.
        entry<PlayerDestination>(metadata = metadata { put(HideMiniPlayer, true) }) {
            val state by controller.state.collectAsStateWithLifecycle()
            val title = (state as? PlaybackState.Active)?.current?.title.orEmpty()
            // Скоупы записи: ViewModel из её ViewModelStore и состояние из её SaveableStateHolder.
            val viewModelId = viewModel { PlayerStubViewModel() }.id
            val savedId = rememberSaveable { nextSavedId.incrementAndGet() }
            Column {
                Text(text = "$PLAYER_STUB_TEXT$title", modifier = Modifier.testTag(PLAYER_STUB_TAG))
                Text(text = "vm=$viewModelId saved=$savedId", modifier = Modifier.testTag(PLAYER_STUB_SCOPES_TAG))
            }
        }
    }

    /** Номер экземпляра: новый номер — новый ViewModelStore записи. */
    class PlayerStubViewModel : ViewModel() {
        val id = nextViewModelId.incrementAndGet()
    }

    private val nextViewModelId = AtomicInteger()
    private val nextSavedId = AtomicInteger()

    const val PLAYER_STUB_TAG = "player_stub"
    const val PLAYER_STUB_TEXT = "Экран плеера: "
    const val PLAYER_STUB_SCOPES_TAG = "player_stub_scopes"
}
