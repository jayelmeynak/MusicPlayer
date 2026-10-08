package com.jayelmeynak.lib.database.data.db

import android.content.ContextWrapper
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * The artwork cache must survive the move of `LocalDatabase` to another module and package.
 * The file below is built with the DDL and the identity hash generated from the code before the
 * move (`com.jayelmeynak.local.data.db`). Room refuses to open a file whose hash differs from
 * the current schema, so a passing test means users keep their cached artworks after update.
 */
class LocalDatabaseCompatibilityTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var dbFile: File
    private var db: LocalDatabase? = null

    @Before
    fun setUp() {
        dbFile = File(tempFolder.root, "local_cache.db")
        BundledSQLiteDriver().open(dbFile.absolutePath).use { connection ->
            connection.execSQL(
                "CREATE TABLE IF NOT EXISTS `artworks` (`track_id` INTEGER NOT NULL, " +
                    "`data` BLOB, `cached_at` INTEGER NOT NULL, PRIMARY KEY(`track_id`))"
            )
            connection.execSQL(
                "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)"
            )
            connection.execSQL(
                "INSERT OR REPLACE INTO room_master_table (id,identity_hash) " +
                    "VALUES(42, '$OLD_IDENTITY_HASH')"
            )
            connection.execSQL(
                "INSERT INTO artworks (track_id, data, cached_at) VALUES (7, x'010203', 100)"
            )
            connection.execSQL("PRAGMA user_version = 1")
        }
    }

    @After
    fun tearDown() {
        db?.close()
    }

    @Test
    fun `база старого кода открывается и отдаёт закэшированную обложку`() = runTest {
        val context = object : ContextWrapper(null) {
            override fun getDatabasePath(name: String): File = File(tempFolder.root, name)
        }
        val database = Room.databaseBuilder(context, LocalDatabase::class.java, dbFile.name)
            .setDriver(BundledSQLiteDriver())
            .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
            .build()
            .also { db = it }

        val entity = database.artworkDao().getByTrackId(7)!!

        assertEquals(7L, entity.trackId)
        assertArrayEquals(byteArrayOf(1, 2, 3), entity.data)
        assertEquals(100L, entity.cachedAt)
    }

    private companion object {
        const val OLD_IDENTITY_HASH = "78d4ce27fefb342237ae5e68520d1f8d"
    }
}
