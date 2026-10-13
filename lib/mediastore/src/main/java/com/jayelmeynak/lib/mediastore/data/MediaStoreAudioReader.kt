package com.jayelmeynak.lib.mediastore.data

import androidx.annotation.WorkerThread

/** Чтение музыки устройства из MediaStore. Нужен доступ к аудио, иначе — `SecurityException`. */
public interface MediaStoreAudioReader {

    /** Музыка без голосовых форматов, по имени файла. Синхронно: звать не с main-потока. */
    @WorkerThread
    public fun readAudio(): List<MediaStoreAudio>

    /**
     * Наблюдение за аудио MediaStore: [onAudioChanged] вызывается при добавлении, изменении или удалении
     * трека (в т.ч. пачкой при сканировании), с любого потока. Наблюдение действует до `close()`
     * результата — его обязан вызвать тот, кто подписался, иначе наблюдатель утечёт.
     */
    public fun observeAudioChanges(onAudioChanged: () -> Unit): AutoCloseable
}
