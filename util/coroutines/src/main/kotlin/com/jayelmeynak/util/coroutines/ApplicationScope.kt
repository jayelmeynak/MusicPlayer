package com.jayelmeynak.util.coroutines

import javax.inject.Qualifier

/**
 * Scope, живущий столько же, сколько процесс приложения.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
public annotation class ApplicationScope
