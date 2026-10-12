package com.jayelmeynak.lib.designsystem.components

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Список треков: строка [TrackItem] на каждый элемент [tracks]. Тап отдаёт в [onTrackClick] сам
 * нарисованный список и индекс строки — очередь плеера строится из того, что видел пользователь,
 * даже если состояние экрана успело смениться. Модель трека — у фичи: список берёт из неё ключ,
 * название и исполнителя через [key], [title], [artistName], обложку рисует слот [image].
 *
 * @param key ключ строки `LazyColumn`: уникален в списке и сохраняем в `Bundle` (примитив,
 *   `String`, `Parcelable`) — по нему список хранит позицию прокрутки при пересоздании.
 * @param supportingText мелкая строка под подписью трека, `null` — нет.
 * @param trailing элемент справа в строке трека (своё нажатие), `null` — нет.
 */
@Composable
public fun <T> TrackList(
    tracks: List<T>,
    key: (T) -> Any,
    title: (T) -> String,
    artistName: (T) -> String,
    onTrackClick: (tracks: List<T>, index: Int) -> Unit,
    modifier: Modifier = Modifier,
    supportingText: (T) -> String? = { null },
    trailing: (@Composable (T) -> Unit)? = null,
    image: @Composable (T) -> Unit,
) {
    LazyColumn(modifier = modifier) {
        itemsIndexed(tracks, key = { _, track -> key(track) }) { index, track ->
            TrackItem(
                title = title(track),
                artistName = artistName(track),
                onClick = { onTrackClick(tracks, index) },
                supportingText = supportingText(track),
                trailing = trailing?.let { slot -> { slot(track) } },
                image = { image(track) },
            )
        }
    }
}
