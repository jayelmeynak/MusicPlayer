package com.jayelmeynak.musicplayer.presentation.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import com.jayelmeynak.feature.catalog.api.ChartDestination
import com.jayelmeynak.feature.localmusic.api.LocalMusicDestination
import com.jayelmeynak.musicplayer.R

/**
 * Вкладка оболочки — идентификатор `app`, а не ключ навигации: в стек не кладётся, сохраняется по
 * имени. [root] — корень её стека, экран из `:api` фичи; корни вкладок различны. Порядок значений —
 * порядок пунктов меню, первая вкладка — стартовая.
 */
enum class AppTab(val root: NavKey, @StringRes val label: Int) {
    Catalog(root = ChartDestination, label = R.string.tab_remote),
    LocalMusic(root = LocalMusicDestination, label = R.string.tab_local),
}

/** Иконка пункта меню; функцией, а не полем: векторы строятся только при отрисовке. */
fun AppTab.icon(selected: Boolean): ImageVector = when (this) {
    AppTab.Catalog -> if (selected) Icons.Filled.Wifi else Icons.Outlined.Wifi
    AppTab.LocalMusic -> if (selected) Icons.Filled.SdStorage else Icons.Outlined.SdStorage
}
