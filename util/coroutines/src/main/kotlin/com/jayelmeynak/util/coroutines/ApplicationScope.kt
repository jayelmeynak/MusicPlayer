package com.jayelmeynak.util.coroutines

import javax.inject.Qualifier

/**
 * Scope that lives as long as the application process.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
public annotation class ApplicationScope
