package com.jayelmeynak.feature.player.impl.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jayelmeynak.feature.player.api.PlayerDestination
import com.jayelmeynak.lib.navigation.Navigator

/**
 * Экран плеера. Показывает индикатор, пока сессия воспроизведения не подключена и не
 * восстановлена, и убирает свой ключ из стека, когда подключённой сессии нечего играть. Сервис
 * плеера экран не стартует: к нему подключается контроллер воспроизведения.
 */
@Composable
internal fun PlayerRoute(
    navigator: Navigator,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                // Свой ключ, не «назад»: поверх экрана плеера мог открыться другой экран.
                PlayerEffect.Close -> navigator.remove(PlayerDestination)
            }
        }
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    PlayerScreen(state = state, onAction = viewModel::onAction)
}
