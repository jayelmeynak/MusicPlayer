package com.jayelmeynak.player.domain.repository

import com.jayelmeynak.network.utils.DataError
import com.jayelmeynak.network.utils.Result
import com.jayelmeynak.player.domain.models.Track

interface MusicRemoteRepository {

    suspend fun getTrack(id: String): Result<Track, DataError.Remote>

    suspend fun getAlbum(id: String): Result<List<Track>, DataError.Remote>
}