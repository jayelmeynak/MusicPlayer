package com.jayelmeynak.lib.mediastore.data

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.provider.MediaStore
import androidx.annotation.WorkerThread
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** [MediaStoreAudioReader] на `ContentResolver`. */
internal class ContentResolverAudioReader @Inject constructor(
    @ApplicationContext private val context: Context,
) : MediaStoreAudioReader {

    @WorkerThread
    override fun readAudio(): List<MediaStoreAudio> = buildList {
        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            PROJECTION,
            SELECTION,
            SELECTION_ARGS,
            SORT_ORDER,
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns._ID)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ARTIST)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DURATION)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.TITLE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                add(
                    MediaStoreAudio(
                        id = id,
                        uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                            .toString(),
                        title = cursor.getString(titleColumn),
                        // У файла без тега исполнителя в MediaStore NULL.
                        artist = cursor.getString(artistColumn) ?: "",
                        durationMs = cursor.getInt(durationColumn),
                    )
                )
            }
        }
    }

    override fun observeAudioChanges(onAudioChanged: () -> Unit): AutoCloseable {
        // Без Handler колбэк приходит на binder-потоке: подписчику поток не важен.
        val observer = object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean, uri: Uri?) = onAudioChanged()
        }
        val resolver = context.contentResolver
        // С потомками: провайдер сообщает об изменении отдельного трека его собственным URI.
        resolver.registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, observer)
        return AutoCloseable { resolver.unregisterContentObserver(observer) }
    }

    private companion object {
        val PROJECTION = arrayOf(
            MediaStore.Audio.AudioColumns._ID,
            MediaStore.Audio.AudioColumns.ARTIST,
            MediaStore.Audio.AudioColumns.DURATION,
            MediaStore.Audio.AudioColumns.TITLE,
        )

        // Только музыка; голосовые записи и диктофон (AMR, 3GPP, AAC) — не треки.
        const val SELECTION =
            "${MediaStore.Audio.AudioColumns.IS_MUSIC} = ? AND ${MediaStore.Audio.Media.MIME_TYPE} NOT IN (?, ?, ?)"
        val SELECTION_ARGS = arrayOf("1", "audio/amr", "audio/3gpp", "audio/aac")

        const val SORT_ORDER = "${MediaStore.Audio.AudioColumns.DISPLAY_NAME} ASC"
    }
}
