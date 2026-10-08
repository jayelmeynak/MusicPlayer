package com.jayelmeynak.lib.network.data.dto

import com.google.gson.annotations.SerializedName

public data class TrackDto(
    @SerializedName("id")
    public val id: Long? = null,
    @SerializedName("title")
    public val title: String? = null,
    @SerializedName("title_short")
    public val titleShort: String? = null,
    @SerializedName("album")
    public val album: AlbumDto? = null,
    @SerializedName("artist")
    public val artist: Artist? = null,
    @SerializedName("duration")
    public val duration: Int? = null,
    @SerializedName("explicit_content_cover")
    public val explicitContentCover: Int? = null,
    @SerializedName("explicit_content_lyrics")
    public val explicitContentLyrics: Int? = null,
    @SerializedName("explicit_lyrics")
    public val explicitLyrics: Boolean? = null,
    @SerializedName("link")
    public val link: String? = null,
    @SerializedName("preview")
    public val preview: String? = null,
)
