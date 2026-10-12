package com.jayelmeynak.feature.catalog.impl.presentation

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jayelmeynak.feature.catalog.impl.R
import com.jayelmeynak.feature.player.api.PlayerDestination
import com.jayelmeynak.lib.navigation.Navigator

/**
 * Экран каталога в навигации: состояние ViewModel и её эффекты. Тап по треку запускает
 * нарисованный список, затем эффект открывает экран плеера — `play` раньше перехода
 * (`docs/playback.md`). Ссылку на страницу трека открывает внешнее приложение; если открыть нечем —
 * короткое сообщение.
 */
@Composable
internal fun CatalogRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: CatalogViewModel = hiltViewModel(),
) {
    // Эффекты собираются один раз на ViewModel, а контекст может смениться (смена конфигурации).
    val currentContext by rememberUpdatedState(LocalContext.current)
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                CatalogEffect.OpenPlayer -> navigator.navigateTo(PlayerDestination)
                is CatalogEffect.OpenLink -> {
                    val context = currentContext
                    if (!openExternalLink(context, effect.link)) {
                        val message = context.getString(R.string.no_app_for_link)
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    CatalogScreen(state = state, onAction = viewModel::onAction, modifier = modifier)
}
