package com.jayelmeynak.musicplayer

import android.app.Application
import com.jayelmeynak.util.coroutines.ApplicationScope
import com.jayelmeynak.util.coroutines.Dispatcher
import com.jayelmeynak.util.coroutines.MusicPlayerDispatchers
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class MyApp : Application() {

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    @Inject
    @Dispatcher(MusicPlayerDispatchers.IO)
    lateinit var ioDispatcher: CoroutineDispatcher

    override fun onCreate() {
        super.onCreate()
        // Прежние кэши исчезают после первого запуска новой версии; дальше удалять нечего — вызовы дешёвые.
        // Отдельные корутины: сбой одной очистки не отменяет другую.
        applicationScope.launch(ioDispatcher) { deleteLegacyArtworkCache() }
        applicationScope.launch(ioDispatcher) { deleteLegacyImageCache() }
    }
}
