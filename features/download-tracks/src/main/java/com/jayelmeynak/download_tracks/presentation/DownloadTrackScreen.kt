package com.jayelmeynak.download_tracks.presentation

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jayelmeynak.download_tracks.presentation.components.TrackItem
import com.jayelmeynak.download_tracks.presentation.components.TrackSearchBar

@Composable
fun DownloadTrackScreen(
    scaffoldPadding: PaddingValues,
    viewModel: DownloadTracksViewModel = hiltViewModel(),
    onTrackClicked: (Uri) -> Unit,
) {
    val state = viewModel.state.value
    DownloadTracks(
        scaffoldPadding = scaffoldPadding,
        state = state,
        onTrackClicked = onTrackClicked,
        onSearchQueryChange = { query ->
            viewModel.onAction(DownloadTracksAction.OnSearchQueryChange(query))
        }
    )

}

@Composable
fun DownloadTracks(
    scaffoldPadding: PaddingValues,
    state: DownloadTracksState,
    onTrackClicked: (Uri) -> Unit,
    onSearchQueryChange: (String) -> Unit,
) {

    val listToDisplay = if (state.searchList.isNotEmpty()) state.searchList else state.tracks
    val keyboardController = LocalSoftwareKeyboardController.current

    when {
        state.isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize().padding(scaffoldPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        state.errorMessage != null -> {
            Box(
                modifier = Modifier.fillMaxSize().padding(scaffoldPadding),
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
                    .padding(scaffoldPadding)
                    .background(MaterialTheme.colorScheme.background),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TrackSearchBar(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
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
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(listToDisplay) { track ->
                            TrackItem(
                                track = track,
                                onTrackClick = {
                                    onTrackClicked(track.uri)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

}