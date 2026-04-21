package com.jayelmeynak.local.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
internal abstract class ArtworkDao {

    @Query("SELECT * FROM artworks WHERE track_id = :trackId LIMIT 1")
    abstract suspend fun getByTrackId(trackId: Long): ArtworkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(entity: ArtworkEntity)

    @Query("DELETE FROM artworks WHERE track_id NOT IN (:activeTrackIds)")
    protected abstract suspend fun deleteStaleInternal(activeTrackIds: List<Long>)

    suspend fun deleteStale(activeTrackIds: List<Long>) {
        if (activeTrackIds.isNotEmpty()) deleteStaleInternal(activeTrackIds)
    }
}
