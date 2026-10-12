package com.jayelmeynak.feature.catalog.impl.presentation

/** Одноразовые сигналы экрана каталога. */
internal sealed interface CatalogEffect {
    /** Очередь уже отдана плееру: открыть экран плеера. */
    data object OpenPlayer : CatalogEffect

    /** Открыть страницу трека [link] во внешнем приложении (Deezer или браузер). */
    data class OpenLink(val link: String) : CatalogEffect
}
