package com.jayelmeynak.feature.localmusic.impl.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jayelmeynak.feature.player.api.PlayerDestination
import com.jayelmeynak.lib.navigation.Navigator

/**
 * Вкладка «Local» в навигации: состояние ViewModel, её эффекты и доступ к аудио. Тап по треку
 * запускает нарисованный список, затем эффект открывает экран плеера — `play` раньше перехода
 * (`docs/playback.md`). Доступ проверяется на каждом `resume`: пользователь может выдать его в
 * настройках и вернуться.
 */
@Composable
internal fun LocalTracksRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: LocalTracksViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> viewModel.onAction(LocalTracksAction.OnAudioPermissionChecked(granted)) }

    LifecycleResumeEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, audioPermission()) ==
                PackageManager.PERMISSION_GRANTED
        viewModel.onAction(LocalTracksAction.OnAudioPermissionChecked(granted))
        onPauseOrDispose { }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                LocalTracksEffect.OpenPlayer -> navigator.navigateTo(PlayerDestination)
            }
        }
    }

    val state by viewModel.state.collectAsStateWithLifecycle()
    LocalTracksScreen(
        state = state,
        onAction = viewModel::onAction,
        onRequestPermission = {
            // Системный диалог, пока его ещё можно показать; после окончательного отказа — настройки приложения.
            if (activity?.shouldShowRequestPermissionRationale(audioPermission()) == true) {
                permissionLauncher.launch(audioPermission())
            } else {
                context.startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)
                    )
                )
            }
        },
        modifier = modifier,
    )
}

private fun audioPermission(): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
