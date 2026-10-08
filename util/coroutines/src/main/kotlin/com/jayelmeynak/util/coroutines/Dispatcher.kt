package com.jayelmeynak.util.coroutines

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
public annotation class Dispatcher(public val dispatcher: MusicPlayerDispatchers)

public enum class MusicPlayerDispatchers {
    Default,
    IO,
    Main,
}
