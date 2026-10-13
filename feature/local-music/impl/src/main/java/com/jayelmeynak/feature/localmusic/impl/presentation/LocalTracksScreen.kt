package com.jayelmeynak.feature.localmusic.impl.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jayelmeynak.feature.localmusic.impl.R
import com.jayelmeynak.lib.designsystem.R as DesignR
import com.jayelmeynak.lib.designsystem.components.LocalTrackImage
import com.jayelmeynak.lib.designsystem.components.TrackList
import com.jayelmeynak.lib.designsystem.components.TrackSearchBar

/**
 * Вкладка «Local» без состояния: строка поиска и то, что велит [LocalTracksUiState.content].
 * Кнопка «нет доступа» — действие: диалог или настройки выбирает VM.
 */
@Composable
internal fun LocalTracksScreen(
    state: LocalTracksUiState,
    onAction: (LocalTracksAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TrackSearchBar(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            searchQuery = state.query,
            onSearchQueryChange = { onAction(LocalTracksAction.OnSearchQueryChange(it)) },
            onImeSearch = {
                keyboardController?.hide()
            }
        )
        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = 8.dp),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 8.dp, start = 8.dp, end = 8.dp),
            ) {
                when (val content = state.content) {
                    is LocalTracksContent.NoAccess -> CenteredBox {
                        Column(
                            modifier = Modifier.padding(horizontal = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            val (message, button) = if (content.permanently) {
                                R.string.local_tracks_permission_denied_permanently to R.string.open_settings
                            } else {
                                R.string.local_tracks_permission_denied to R.string.grant_access
                            }
                            Text(text = stringResource(message), textAlign = TextAlign.Center)
                            Button(
                                onClick = { onAction(LocalTracksAction.OnGrantAccessClick) },
                                modifier = Modifier.padding(top = 16.dp),
                            ) {
                                Text(text = stringResource(button))
                            }
                        }
                    }

                    LocalTracksContent.Loading -> CenteredBox { CircularProgressIndicator() }

                    is LocalTracksContent.Error -> CenteredBox {
                        Text(text = content.message.asString())
                    }

                    LocalTracksContent.NothingFound -> CenteredBox {
                        Text(text = stringResource(DesignR.string.search_nothing_found))
                    }

                    LocalTracksContent.Empty -> CenteredBox {
                        Text(text = stringResource(R.string.local_tracks_empty))
                    }

                    is LocalTracksContent.Tracks -> TrackList(
                        tracks = content.tracks,
                        key = { it.id },
                        title = { it.title },
                        artistName = { it.artistName },
                        onTrackClick = { tracks, index -> onAction(LocalTracksAction.OnTrackClick(tracks, index)) },
                        image = { LocalTrackImage(it.uri) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CenteredBox(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        content()
    }
}
