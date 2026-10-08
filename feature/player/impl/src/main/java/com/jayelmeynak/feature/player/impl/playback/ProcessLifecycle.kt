package com.jayelmeynak.feature.player.impl.playback

import javax.inject.Qualifier

/** The `LifecycleOwner` of the whole app process: started while any Activity is visible. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class ProcessLifecycle
