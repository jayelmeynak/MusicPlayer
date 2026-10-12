package com.jayelmeynak.feature.catalog.impl.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jayelmeynak.feature.catalog.impl.R as CatalogR
import com.jayelmeynak.lib.designsystem.R
import com.jayelmeynak.lib.designsystem.components.RemoteTrackImage
import com.jayelmeynak.lib.designsystem.components.TrackList
import com.jayelmeynak.lib.designsystem.components.TrackSearchBar

/** Экран каталога без состояния: строка поиска и то, что велит [CatalogUiState.content]. */
@Composable
internal fun CatalogScreen(
    state: CatalogUiState,
    onAction: (CatalogAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val previewLabel = stringResource(CatalogR.string.preview_label)

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
            onSearchQueryChange = { onAction(CatalogAction.OnSearchQueryChange(it)) },
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
                    CatalogContent.Loading -> CenteredBox { CircularProgressIndicator() }

                    is CatalogContent.Error -> CenteredBox {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = content.message.asString())
                            if (content.canRetry) {
                                Button(
                                    onClick = { onAction(CatalogAction.OnRetryClick) },
                                    modifier = Modifier.padding(top = 16.dp)
                                ) {
                                    Text(text = stringResource(CatalogR.string.retry))
                                }
                            }
                        }
                    }

                    CatalogContent.NothingFound -> CenteredBox {
                        Text(text = stringResource(R.string.search_nothing_found))
                    }

                    CatalogContent.ChartEmpty -> CenteredBox {
                        Text(text = stringResource(CatalogR.string.chart_empty))
                    }

                    is CatalogContent.Tracks -> TrackList(
                        tracks = content.tracks,
                        key = { it.id },
                        title = { it.title },
                        // Реальная длительность и пометка превью (ADR-15): играет 30-секундный отрывок.
                        artistName = { formatTrackSubtitle(it.artistName, it.duration) },
                        supportingText = { previewLabel },
                        trailing = { track ->
                            track.link?.let { link ->
                                OpenInDeezerButton(
                                    trackTitle = track.title,
                                    onClick = { onAction(CatalogAction.OnOpenInDeezerClick(link)) },
                                )
                            }
                        },
                        onTrackClick = { tracks, index -> onAction(CatalogAction.OnTrackClick(tracks, index)) },
                        image = { RemoteTrackImage(it.coverUrl) },
                    )
                }
            }
        }
    }
}

/** Открыть страницу трека на Deezer; описание с названием трека — для TalkBack. */
@Composable
private fun OpenInDeezerButton(trackTitle: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = stringResource(CatalogR.string.open_in_deezer, trackTitle),
        )
    }
}

@Composable
private fun CenteredBox(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        content()
    }
}
