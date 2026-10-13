package com.jayelmeynak.lib.mediastore.data

import androidx.annotation.WorkerThread

/** Чтение музыки устройства из MediaStore. Нужен доступ к аудио, иначе — `SecurityException`. */
public interface MediaStoreAudioReader {

    /** Музыка без голосовых форматов, по имени файла. Синхронно: звать не с main-потока. */
    @WorkerThread
    public fun readAudio(): List<MediaStoreAudio>
}
