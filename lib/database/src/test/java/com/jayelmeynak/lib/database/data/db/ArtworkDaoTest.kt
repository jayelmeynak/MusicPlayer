package com.jayelmeynak.lib.database.data.db

import android.content.ContextWrapper
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Host JVM test on [BundledSQLiteDriver], without Robolectric. Room 2.8 on Android needs a
 * Context, but with a driver and an explicit journal mode it never touches it.
 */
class ArtworkDaoTest {

    private lateinit var db: LocalDatabase
    private lateinit var dao: ArtworkDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ContextWrapper(null), LocalDatabase::class.java)
            .setDriver(BundledSQLiteDriver())
            .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
            .build()
        dao = db.artworkDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `нет записи - null`() = runTest {
        assertNull(dao.getByTrackId(1))
    }

    @Test
    fun `вставленная запись читается по trackId`() = runTest {
        dao.insert(ArtworkEntity(trackId = 1, data = byteArrayOf(1, 2, 3), cachedAt = 100))

        assertEquals(
            ArtworkEntity(trackId = 1, data = byteArrayOf(1, 2, 3), cachedAt = 100),
            dao.getByTrackId(1),
        )
    }

    @Test
    fun `null-обложка кэшируется как запись с data = null`() = runTest {
        dao.insert(ArtworkEntity(trackId = 1, data = null, cachedAt = 100))

        val entity = dao.getByTrackId(1)
        assertNotNull(entity)
        assertNull(entity!!.data)
    }

    @Test
    fun `повторная вставка с тем же trackId заменяет запись`() = runTest {
        dao.insert(ArtworkEntity(trackId = 1, data = byteArrayOf(1), cachedAt = 100))
        dao.insert(ArtworkEntity(trackId = 1, data = byteArrayOf(9), cachedAt = 200))

        val entity = dao.getByTrackId(1)!!
        assertArrayEquals(byteArrayOf(9), entity.data)
        assertEquals(200, entity.cachedAt)
    }

    @Test
    fun `deleteStale оставляет только актуальные trackId`() = runTest {
        listOf(1L, 2L, 3L).forEach { dao.insert(ArtworkEntity(trackId = it, data = null)) }

        dao.deleteStale(listOf(1L, 3L))

        assertNotNull(dao.getByTrackId(1))
        assertNull(dao.getByTrackId(2))
        assertNotNull(dao.getByTrackId(3))
    }

    @Test
    fun `deleteStale с пустым списком ничего не удаляет`() = runTest {
        listOf(1L, 2L).forEach { dao.insert(ArtworkEntity(trackId = it, data = null)) }

        dao.deleteStale(emptyList())

        assertNotNull(dao.getByTrackId(1))
        assertNotNull(dao.getByTrackId(2))
    }
}
