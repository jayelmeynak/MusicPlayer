package com.jayelmeynak.feature.localmusic.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Корень стека вкладки «Local»: аудиофайлы устройства из MediaStore. */
@Serializable
public data object LocalMusicDestination : NavKey
