package com.jayelmeynak.lib.database.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

internal const val LOCAL_DATABASE_NAME = "local_cache.db"

@Database(
    entities = [ArtworkEntity::class],
    version = 1,
    exportSchema = false,
)
internal abstract class LocalDatabase : RoomDatabase() {
    abstract fun artworkDao(): ArtworkDao
}
