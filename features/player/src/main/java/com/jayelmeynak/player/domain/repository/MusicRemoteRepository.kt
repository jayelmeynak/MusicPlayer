package com.jayelmeynak.player.domain.repository

import com.jayelmeynak.util.result.DataError
import com.jayelmeynak.util.result.Result
import com.jayelmeynak.player.domain.models.Track

internal interface MusicRemoteRepository {

    suspend fun getTrack(id: String): Result<Track, DataError.Remote>

    suspend fun getAlbum(id: String): Result<List<Track>, DataError.Remote>
}