package com.jayelmeynak.lib.database.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
public abstract class ArtworkDao {

    @Query("SELECT * FROM artworks WHERE track_id = :trackId LIMIT 1")
    public abstract suspend fun getByTrackId(trackId: Long): ArtworkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract suspend fun insert(entity: ArtworkEntity)

    @Query("DELETE FROM artworks WHERE track_id NOT IN (:activeTrackIds)")
    protected abstract suspend fun deleteStaleInternal(activeTrackIds: List<Long>)

    public suspend fun deleteStale(activeTrackIds: List<Long>) {
        if (activeTrackIds.isNotEmpty()) deleteStaleInternal(activeTrackIds)
    }
}
