package com.jayelmeynak.feature.catalog.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Экран каталога Deezer: чарт и строка поиска над ним. */
@Serializable
public data object ChartDestination : NavKey
