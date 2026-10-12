package com.jayelmeynak.feature.catalog.impl.presentation

import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jayelmeynak.lib.designsystem.R
import com.jayelmeynak.lib.designsystem.components.RemoteTrackImage
import com.jayelmeynak.lib.designsystem.components.TrackItem
import com.jayelmeynak.lib.designsystem.components.TrackSearchBar
import com.jayelmeynak.feature.catalog.impl.R as CatalogR
import com.jayelmeynak.feature.player.api.PlayerDestination
import com.jayelmeynak.lib.navigation.Navigator

/**
 * Экран каталога: строка поиска и список (чарт или выдача поиска). Тап по треку запускает видимый
 * список с этого трека и открывает экран плеера — `play` раньше перехода (`docs/playback.md`).
 */
@Composable
internal fun ChartTracksScreen(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: ChartTracksViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val errorMessage = state.errorMessage
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
            onSearchQueryChange = { viewModel.onAction(ChartTracksAction.OnSearchQueryChange(it)) },
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
                when {
                    state.isLoading -> CenteredBox { CircularProgressIndicator() }

                    errorMessage != null -> CenteredBox {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = errorMessage.asString())
                            if (state.isChartError) {
                                Button(
                                    onClick = { viewModel.onAction(ChartTracksAction.OnRetryClick) },
                                    modifier = Modifier.padding(top = 16.dp)
                                ) {
                                    Text(text = stringResource(CatalogR.string.retry))
                                }
                            }
                        }
                    }

                    state.searchList?.isEmpty() == true -> CenteredBox {
                        Text(text = stringResource(R.string.search_nothing_found))
                    }

                    state.isChartEmpty -> CenteredBox {
                        Text(text = stringResource(CatalogR.string.chart_empty))
                    }

                    else -> LazyColumn {
                        val tracks = state.searchList ?: state.charts
                        itemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
                            TrackItem(
                                title = track.title,
                                artistName = track.artistName,
                                onClick = {
                                    viewModel.onAction(ChartTracksAction.OnTrackClick(tracks, index))
                                    navigator.navigateTo(PlayerDestination)
                                },
                                image = { RemoteTrackImage(track.coverUrl) }
                            )
                        }
                    }
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
