package com.jayelmeynak.feature.localmusic.impl.resolver

import com.jayelmeynak.feature.player.api.TrackSource
import com.jayelmeynak.feature.player.api.TrackUriResolver
import com.jayelmeynak.lib.mediastore.data.isMediaStoreUri
import javax.inject.Inject

/**
 * Резолвер источника `LOCAL` для плеера: id локального трека — его `content://` URI в MediaStore,
 * он и воспроизводится; любой другой id (чужой провайдер, `file://`, ссылка) — `null`. Это вторая из
 * двух защит: сессия не играет URI, принесённый элементом очереди (до резолвера доходит только ключ
 * трека из `mediaId`), а этот резолвер открывает только строки MediaStore.
 *
 * `public` только ради привязки `@IntoSet` в `:di`.
 */
public class LocalContentUriResolver @Inject internal constructor() : TrackUriResolver {

    override val source: TrackSource = TrackSource.LOCAL

    override suspend fun resolve(id: String): String? = id.takeIf(::isMediaStoreUri)
}
