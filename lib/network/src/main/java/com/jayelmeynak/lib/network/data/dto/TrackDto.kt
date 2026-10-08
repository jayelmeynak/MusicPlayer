package com.jayelmeynak.lib.network.data.dto

import com.google.gson.annotations.SerializedName

public data class TrackDto(
    @SerializedName("id")
    public val id: Long,
    @SerializedName("title")
    public val title: String,
    @SerializedName("title_short")
    public val titleShort: String,
    @SerializedName("album")
    public val album: AlbumDto,
    @SerializedName("artist")
    public val artist: Artist,
    @SerializedName("duration")
    public val duration: Int,
    @SerializedName("explicit_content_cover")
    public val explicitContentCover: Int,
    @SerializedName("explicit_content_lyrics")
    public val explicitContentLyrics: Int,
    @SerializedName("explicit_lyrics")
    public val explicitLyrics: Boolean,
    @SerializedName("link")
    public val link: String,
    @SerializedName("preview")
    public val preview: String,
)