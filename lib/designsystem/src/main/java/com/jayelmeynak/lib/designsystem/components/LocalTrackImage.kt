package com.jayelmeynak.lib.designsystem.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.jayelmeynak.lib.designsystem.R

@Composable
public fun LocalTrackImage(
    artwork: ByteArray?,
    modifier: Modifier = Modifier,
) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(artwork)
            .error(R.drawable.track_place_holder)
            .fallback(R.drawable.track_place_holder)
            .crossfade(true)
            .build(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(56.dp)
            .clip(RoundedCornerShape(8.dp))
    )
}
