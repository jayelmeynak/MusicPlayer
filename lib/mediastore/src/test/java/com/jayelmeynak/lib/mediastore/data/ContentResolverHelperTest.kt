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
class ContentResolverHelperTest {

    @Before
    fun setUp() {
        Robolectric.setupContentProvider(FakeMediaProvider::class.java, MediaStore.AUTHORITY)
    }

    @Test
    fun `трек без исполнителя читается с пустым исполнителем, остальные треки на месте`() {
        val helper = ContentResolverHelper(RuntimeEnvironment.getApplication())

        val tracks = helper.getAudioData()

        assertEquals(
            listOf(
                TrackDbo(audioUri(1), 1, "", 180_000, "Без исполнителя"),
                TrackDbo(audioUri(2), 2, "Linkin Park", 200_000, "Numb"),
            ),
            tracks,
        )
    }

    private fun audioUri(id: Long): Uri =
        ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

    class FakeMediaProvider : ContentProvider() {
        override fun onCreate(): Boolean = true

        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?,
        ): Cursor = MatrixCursor(projection).apply {
            addRow(arrayOf<Any?>(1L, null, 180_000, "Без исполнителя"))
            addRow(arrayOf<Any?>(2L, "Linkin Park", 200_000, "Numb"))
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
