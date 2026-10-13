package com.jayelmeynak.lib.designsystem.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/** Обложка в строке локального трека: [artworkUri] — `content://` URI трека (эскиз провайдера). */
@Composable
public fun LocalTrackImage(
    artworkUri: String?,
    modifier: Modifier = Modifier,
) {
    TrackArtwork(
        artworkUri = artworkUri,
        modifier = modifier
            .size(56.dp)
            .clip(RoundedCornerShape(8.dp)),
    )
}
