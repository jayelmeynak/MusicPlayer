package com.jayelmeynak.search_tracks.presentation

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jayelmeynak.search_tracks.presentation.components.TrackItem
import com.jayelmeynak.search_tracks.presentation.components.TrackSearchBar

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun ChartTracksScreen(
    scaffoldPadding: PaddingValues,
    viewModel: ChartTracksViewModel = hiltViewModel(),
    onTrackClicked: (String) -> Unit
) {
    val state = viewModel.state.value
    val listToDisplay = if (state.searchList.isNotEmpty()) state.searchList else state.charts
    val keyboardController = LocalSoftwareKeyboardController.current

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
                modifier = Modifier.fillMaxSize().padding(top = 8.dp, start = 8.dp, end = 8.dp),
            ) {
                LazyColumn {
                    items(listToDisplay) { track ->
                        TrackItem(
                            track = track,
                            onTrackClick = {
                                viewModel.onAction(
                                    ChartTracksAction.OnTrackClicked(
                                        track.id.toString()
                                    )
                                )
                                onTrackClicked(track.id.toString())
                            }
                        )
                    }
                }
            }
        }
    }
}