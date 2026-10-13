package com.jayelmeynak.feature.localmusic.impl.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/** Разрешение на чтение аудио устройства: `READ_MEDIA_AUDIO` с API 33, ниже — `READ_EXTERNAL_STORAGE`. */
internal fun audioReadPermission(): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

/** Выдано ли приложению [audioReadPermission]. */
internal fun Context.hasAudioReadPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, audioReadPermission()) == PackageManager.PERMISSION_GRANTED
