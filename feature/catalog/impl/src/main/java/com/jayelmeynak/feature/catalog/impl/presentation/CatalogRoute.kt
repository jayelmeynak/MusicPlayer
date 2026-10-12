package com.jayelmeynak.feature.catalog.impl.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jayelmeynak.feature.player.api.PlayerDestination
import com.jayelmeynak.lib.navigation.Navigator

/**
 * Экран каталога в навигации: состояние ViewModel и её эффекты. Тап по треку запускает
 * нарисованный список, затем эффект открывает экран плеера — `play` раньше перехода
 * (`docs/playback.md`).
 */
@Composable
internal fun CatalogRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: CatalogViewModel = hiltViewModel(),
) {
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                CatalogEffect.OpenPlayer -> navigator.navigateTo(PlayerDestination)
            }
        }
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    CatalogScreen(state = state, onAction = viewModel::onAction, modifier = modifier)
}
