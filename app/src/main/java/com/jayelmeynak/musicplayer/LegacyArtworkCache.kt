package com.jayelmeynak.musicplayer

import android.content.Context

/** Файл Room-кэша обложек, который вели версии до перехода на эскизы MediaStore. */
private const val LEGACY_ARTWORK_CACHE_DATABASE = "local_cache.db"

/**
 * Удаляет файл прежнего кэша обложек вместе с журналами (`-wal`, `-shm`, `-journal`): базу больше
 * никто не открывает, а полноразмерные обложки в ней занимали бы место навсегда. `false` — файла нет.
 * Работает с диском: звать не с main.
 */
internal fun Context.deleteLegacyArtworkCache(): Boolean = deleteDatabase(LEGACY_ARTWORK_CACHE_DATABASE)
