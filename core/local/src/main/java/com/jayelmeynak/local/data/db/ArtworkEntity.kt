package com.jayelmeynak.local.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "artworks")
internal data class ArtworkEntity(
    @PrimaryKey
    @ColumnInfo(name = "track_id")
    val trackId: Long,

    @ColumnInfo(name = "data", typeAffinity = ColumnInfo.BLOB)
    val data: ByteArray?,

    @ColumnInfo(name = "cached_at")
    val cachedAt: Long = System.currentTimeMillis(),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ArtworkEntity) return false
        return trackId == other.trackId &&
                cachedAt == other.cachedAt &&
                (data == null) == (other.data == null) &&
                (data == null || data.contentEquals(other.data!!))
    }

    override fun hashCode(): Int {
        var result = trackId.hashCode()
        result = 31 * result + cachedAt.hashCode()
        result = 31 * result + (data?.contentHashCode() ?: 0)
        return result
    }
}
