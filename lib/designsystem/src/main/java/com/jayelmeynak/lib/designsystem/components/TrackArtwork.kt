package com.jayelmeynak.lib.designsystem.components

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.error
import coil3.request.fallback
import coil3.request.placeholder
import com.jayelmeynak.lib.designsystem.R

/**
 * Обложка трека: [artworkUri] — адрес картинки или `content://` URI, обложка которого — эскиз
 * провайдера (у локального трека — URI самого трека). Пока грузится, при ошибке и без обложки —
 * заглушка. Размер и форму задаёт [modifier].
 */
@Composable
public fun TrackArtwork(
    artworkUri: String?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val request = remember(artworkUri, context) { trackArtworkRequest(context, artworkUri) }
    AsyncImage(
        model = request,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier,
    )
}

/**
 * Запрос Coil обложки: `content://` — эскиз провайдера своим загрузчиком и ключом кэша памяти,
 * остальное — стандартные загрузчики Coil; заглушка при загрузке, ошибке и `null`.
 */
internal fun trackArtworkRequest(context: Context, artworkUri: String?): ImageRequest {
    val data = artworkRequestData(artworkUri)
    return ImageRequest.Builder(context)
        .data(data)
        .apply {
            if (data is ContentThumbnail) {
                fetcherFactory(ContentThumbnailFetcher.Factory())
                // Своя модель Coil не умеет делать ключ кэша памяти: ключ — сам URI.
                memoryCacheKey("content-thumbnail:${data.uri}")
            }
        }
        .placeholder(R.drawable.track_place_holder)
        .error(R.drawable.track_place_holder)
        .fallback(R.drawable.track_place_holder)
        .crossfade(true)
        .build()
}

/**
 * Данные запроса Coil: `content://` — эскиз провайдера (стандартный загрузчик Coil прочитал бы
 * аудиофайл как картинку), остальное — как есть.
 */
internal fun artworkRequestData(artworkUri: String?): Any? = when {
    artworkUri == null -> null
    artworkUri.startsWith("${ContentResolver.SCHEME_CONTENT}://") -> ContentThumbnail(Uri.parse(artworkUri))
    else -> artworkUri
}
