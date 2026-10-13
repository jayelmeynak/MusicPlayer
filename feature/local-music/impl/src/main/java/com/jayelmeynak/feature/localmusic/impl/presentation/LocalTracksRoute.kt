package com.jayelmeynak.feature.localmusic.impl.presentation

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jayelmeynak.feature.localmusic.impl.permission.audioReadPermission
import com.jayelmeynak.feature.localmusic.impl.permission.hasAudioReadPermission
import com.jayelmeynak.feature.player.api.PlayerDestination
import com.jayelmeynak.lib.navigation.Navigator

/**
 * Вкладка «Local» в навигации: состояние ViewModel и исполнение её эффектов. Тап по треку запускает
 * нарисованный список, затем эффект открывает экран плеера — `play` раньше перехода
 * (`docs/playback.md`). Доступ к аудио проверяется на каждом `resume` (пользователь мог выдать его в
 * настройках); запросить его или открыть настройки решает VM, экран только выполняет.
 */
@Composable
internal fun LocalTracksRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: LocalTracksViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    // Rationale читается только у Activity; экран встроен в навигацию MainActivity, без Activity — ошибка встраивания.
    val activity = checkNotNull(LocalActivity.current) { "LocalTracksRoute показывается только внутри Activity" }
    // Rationale в момент запроса: переживает поворот, пока диалог открыт.
    var rationaleBeforeRequest by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onAction(
            LocalTracksAction.OnAudioPermissionResult(
                granted = granted,
                rationaleBeforeRequest = rationaleBeforeRequest,
                rationaleAfterRequest = activity.shouldShowRequestPermissionRationale(audioReadPermission()),
            )
        )
    }

    LifecycleResumeEffect(Unit) {
        viewModel.onAction(LocalTracksAction.OnAudioPermissionChecked(context.hasAudioReadPermission()))
        onPauseOrDispose { }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                LocalTracksEffect.OpenPlayer -> navigator.navigateTo(PlayerDestination)
                LocalTracksEffect.RequestAudioPermission -> {
                    rationaleBeforeRequest = activity.shouldShowRequestPermissionRationale(audioReadPermission())
                    permissionLauncher.launch(audioReadPermission())
                }

                LocalTracksEffect.OpenAppSettings -> try {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null)
                        )
                    )
                } catch (e: ActivityNotFoundException) {
                    // Прошивка без экрана настроек приложения: экран «нет доступа» остаётся, без падения.
                    Log.w(TAG, "Нет экрана настроек приложения", e)
                }
            }
        }
    }

    val state by viewModel.state.collectAsStateWithLifecycle()
    LocalTracksScreen(
        state = state,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}

private const val TAG = "LocalTracksRoute"
