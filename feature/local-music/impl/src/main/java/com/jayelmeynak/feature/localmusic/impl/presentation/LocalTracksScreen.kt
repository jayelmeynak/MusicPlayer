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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jayelmeynak.feature.localmusic.impl.R
import com.jayelmeynak.feature.localmusic.impl.domain.model.LocalTrack
import com.jayelmeynak.feature.player.api.PlayerDestination
import com.jayelmeynak.lib.navigation.Navigator
import com.jayelmeynak.lib.designsystem.R as DesignR
import com.jayelmeynak.lib.designsystem.components.LocalTrackImage
import com.jayelmeynak.lib.designsystem.components.TrackItem
import com.jayelmeynak.lib.designsystem.components.TrackSearchBar

/** Экран вкладки «Local»: тап по треку запускает нарисованный список и открывает плеер. */
@Composable
internal fun LocalTracksScreen(
    navigator: Navigator,
    viewModel: LocalTracksViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = LocalActivity.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> viewModel.onAction(LocalTracksAction.OnAudioPermissionChecked(granted)) }

    // Проверка на каждом resume: пользователь может выдать доступ в настройках и вернуться.
    LifecycleResumeEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, audioPermission()) ==
                PackageManager.PERMISSION_GRANTED
        viewModel.onAction(LocalTracksAction.OnAudioPermissionChecked(granted))
        onPauseOrDispose { }
    }

    LocalTracksContent(
        state = state,
        onTrackClicked = { tracks, index ->
            // play — синхронно в onAction, затем переход: порядок «play → плеер» сохраняется.
            viewModel.onAction(LocalTracksAction.OnTrackClick(tracks, index))
            navigator.navigateTo(PlayerDestination)
        },
        onSearchQueryChange = { query ->
            viewModel.onAction(LocalTracksAction.OnSearchQueryChange(query))
        },
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
        }
    )

}

private fun audioPermission(): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

@Composable
private fun LocalTracksContent(
    state: LocalTracksState,
    onTrackClicked: (tracks: List<LocalTrack>, index: Int) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onRequestPermission: () -> Unit,
) {

    val keyboardController = LocalSoftwareKeyboardController.current

    when {
        state.isPermissionDenied -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = stringResource(R.string.local_tracks_permission_denied))
                Button(onClick = onRequestPermission, modifier = Modifier.padding(top = 16.dp)) {
                    Text(text = stringResource(R.string.grant_access))
                }
            }
        }

        state.isLoading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        state.errorMessage != null -> {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = state.errorMessage.asString()
                )
            }
        }

        else -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TrackSearchBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    searchQuery = state.query,
                    onSearchQueryChange = { onSearchQueryChange(it) },
                    onImeSearch = {
                        keyboardController?.hide()
                    }
                )
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(top = 8.dp, start = 8.dp, end = 8.dp),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    if (state.searchList?.isEmpty() == true) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = stringResource(DesignR.string.search_nothing_found))
                        }
                    } else if (state.isLibraryEmpty) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = stringResource(R.string.local_tracks_empty))
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            val tracks = state.searchList ?: state.tracks
                            itemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
                                TrackItem(
                                    title = track.title,
                                    artistName = track.artistName,
                                    onClick = {
                                        onTrackClicked(tracks, index)
                                    },
                                    image = { LocalTrackImage(state.artworks[track.id]) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}