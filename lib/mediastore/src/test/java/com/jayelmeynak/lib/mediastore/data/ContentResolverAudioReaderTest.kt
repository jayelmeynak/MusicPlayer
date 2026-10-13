package com.jayelmeynak.lib.mediastore.data

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.MediaStore
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

// Robolectric — ради настоящего ContentResolver: курсор MediaStore отдаёт тестовый провайдер.
@RunWith(RobolectricTestRunner::class)
class ContentResolverAudioReaderTest {

    private lateinit var provider: FakeMediaProvider

    @Before
    fun setUp() {
        provider = Robolectric.setupContentProvider(FakeMediaProvider::class.java, MediaStore.AUTHORITY)
    }

    @Test
    fun `трек без исполнителя читается с пустым исполнителем, остальные треки на месте`() {
        val tracks = reader().readAudio()

        assertEquals(
            listOf(
                MediaStoreAudio(1, audioUri(1), "Без исполнителя", "", 180_000),
                MediaStoreAudio(2, audioUri(2), "Numb", "Linkin Park", 200_000),
            ),
            tracks,
        )
    }

    @Test
    fun `запрос - только музыка без голосовых форматов, по имени файла`() {
        reader().readAudio()

        val query = provider.queries.single()
        assertEquals(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, query.uri)
        assertEquals("is_music = ? AND mime_type NOT IN (?, ?, ?)", query.selection)
        assertEquals(listOf("1", "audio/amr", "audio/3gpp", "audio/aac"), query.selectionArgs)
        assertEquals("_display_name ASC", query.sortOrder)
    }

    @Test
    fun `пустая медиатека - пустой список`() {
        provider.rows = emptyList()

        val tracks = reader().readAudio()

        assertEquals(emptyList<MediaStoreAudio>(), tracks)
    }

    private fun reader() = ContentResolverAudioReader(RuntimeEnvironment.getApplication())

    private fun audioUri(id: Long): String =
        ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id).toString()

    data class Query(val uri: Uri, val selection: String?, val selectionArgs: List<String>?, val sortOrder: String?)

    class FakeMediaProvider : ContentProvider() {
        val queries = mutableListOf<Query>()

        var rows: List<Array<Any?>> = listOf(
            arrayOf(1L, null, 180_000, "Без исполнителя"),
            arrayOf(2L, "Linkin Park", 200_000, "Numb"),
        )

        override fun onCreate(): Boolean = true

        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?,
        ): Cursor {
            queries += Query(uri, selection, selectionArgs?.toList(), sortOrder)
            return MatrixCursor(projection).apply { rows.forEach(::addRow) }
        }

        override fun getType(uri: Uri): String? = null
        override fun insert(uri: Uri, values: ContentValues?): Uri? = null
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
        override fun update(
            uri: Uri,
            values: ContentValues?,
            selection: String?,
            selectionArgs: Array<out String>?,
        ): Int = 0
    }
}
