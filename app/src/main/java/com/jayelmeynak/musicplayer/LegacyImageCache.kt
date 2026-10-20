package com.jayelmeynak.musicplayer

import android.content.Context
import java.io.File

/** Каталог дискового кэша Coil 2 по умолчанию; у Coil 3 свой каталог — `coil3_disk_cache`. */
private const val LEGACY_IMAGE_CACHE_DIRECTORY = "image_cache"

/**
 * Удаляет каталог дискового кэша Coil 2 со всем содержимым: после перехода на Coil 3 его никто не
 * читает и не чистит. `false` — каталога нет или удалить его целиком не вышло; исключений нет.
 * Работает с диском: звать не с main.
 */
internal fun Context.deleteLegacyImageCache(): Boolean {
    val directory = File(cacheDir, LEGACY_IMAGE_CACHE_DIRECTORY)
    return directory.exists() && directory.deleteRecursively()
}
