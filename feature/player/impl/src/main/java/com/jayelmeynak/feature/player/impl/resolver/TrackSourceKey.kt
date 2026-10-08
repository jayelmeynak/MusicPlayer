package com.jayelmeynak.feature.player.impl.resolver

import com.jayelmeynak.feature.player.api.TrackSource
import dagger.MapKey

/** Map key of a [com.jayelmeynak.feature.player.api.TrackUriResolver] binding. */
@MapKey
@Retention(AnnotationRetention.RUNTIME)
internal annotation class TrackSourceKey(val value: TrackSource)
