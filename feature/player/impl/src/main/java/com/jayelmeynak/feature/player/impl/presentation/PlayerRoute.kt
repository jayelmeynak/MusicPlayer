package com.jayelmeynak.feature.player.impl.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jayelmeynak.feature.player.api.PlayerDestination
import com.jayelmeynak.lib.navigation.Navigator

/**
 * Экран плеера. Показывает индикатор, пока сессия воспроизведения не подключена и не
 * восстановлена, и убирает свой ключ из стека, когда подключённой сессии нечего играть. Сервис
 * плеера экран не стартует: к нему подключается контроллер воспроизведения.
 *
 * На API 33+ при показе сообщает VM, есть ли разрешение на уведомления; спросить ли его, решает VM.
 */
@Composable
internal fun PlayerRoute(
    navigator: Navigator,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    // Исход не важен: без разрешения воспроизведение работает, только уведомление не видно.
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(viewModel) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            viewModel.onAction(PlayerAction.OnNotificationPermissionChecked(granted))
        }
        viewModel.effects.collect { effect ->
            when (effect) {
                // Свой ключ, не «назад»: поверх экрана плеера мог открыться другой экран.
                PlayerEffect.Close -> navigator.remove(PlayerDestination)
                PlayerEffect.RequestNotificationPermission ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
            }
        }
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    PlayerScreen(state = state, onAction = viewModel::onAction)
}
