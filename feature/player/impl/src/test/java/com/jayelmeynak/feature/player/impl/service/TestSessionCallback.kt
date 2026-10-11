package com.jayelmeynak.feature.player.impl.service

import com.jayelmeynak.feature.player.impl.persistence.FakePlaybackStateStore
import com.jayelmeynak.feature.player.impl.persistence.PlaybackResumption
import com.jayelmeynak.feature.player.impl.persistence.PlaybackStateStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Колбэк сессии для тестов: возобновление — из [store], корутины — на главном лупере Robolectric. */
internal fun testSessionCallback(store: PlaybackStateStore = FakePlaybackStateStore()): PlaybackSessionCallback =
    PlaybackSessionCallback(
        resumption = PlaybackResumption(store),
        scope = CoroutineScope(SupervisorJob()),
        mainDispatcher = Dispatchers.Main.immediate,
    )
