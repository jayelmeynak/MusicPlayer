package com.jayelmeynak.search_tracks.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.jayelmeynak.search_tracks.domain.models.Track

@Composable
fun ChartTracksScreen(
    scaffoldPadding: PaddingValues,
    viewModel: ChartTracksViewModel = hiltViewModel(),
    /** The visible list (chart or search results) and the index of the tapped track in it. */
    onTrackClicked: (tracks: List<Track>, index: Int) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val errorMessage = state.errorMessage
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(scaffoldPadding)
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
                        Text(text = errorMessage.asString())
                    }

                    state.searchList?.isEmpty() == true -> CenteredBox {
                        Text(text = stringResource(R.string.search_nothing_found))
                    }

                    else -> LazyColumn {
                        val tracks = state.searchList ?: state.charts
                        itemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
                            TrackItem(
                                title = track.title,
                                artistName = track.artistName,
                                onClick = {
                                    viewModel.onAction(
                                        ChartTracksAction.OnTrackClicked(
                                            track.id.toString()
                                        )
                                    )
                                    onTrackClicked(tracks, index)
                                },
                                image = { RemoteTrackImage(track.album.cover) }
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
