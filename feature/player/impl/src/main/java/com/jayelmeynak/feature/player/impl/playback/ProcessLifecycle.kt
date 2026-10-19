package com.jayelmeynak.feature.player.impl.playback

import javax.inject.Qualifier

/** `LifecycleOwner` всего процесса приложения: в STARTED, пока видна хоть одна Activity. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class ProcessLifecycle
