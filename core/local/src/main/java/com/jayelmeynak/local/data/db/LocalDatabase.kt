package com.jayelmeynak.local.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ArtworkEntity::class],
    version = 1,
    exportSchema = false,
)
internal abstract class LocalDatabase : RoomDatabase() {
    abstract fun artworkDao(): ArtworkDao
}
