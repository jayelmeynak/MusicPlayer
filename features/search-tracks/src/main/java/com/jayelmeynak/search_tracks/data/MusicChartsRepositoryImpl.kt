package com.jayelmeynak.search_tracks.data

import com.jayelmeynak.network.data.RemoteChartDataSource
import com.jayelmeynak.network.utils.DataError
import com.jayelmeynak.network.utils.Result
import com.jayelmeynak.network.utils.map
import com.jayelmeynak.search_tracks.domain.models.Track
import com.jayelmeynak.search_tracks.domain.repositories.MusicChartsRepository

class MusicChartsRepositoryImpl(
    private val remoteMusicDataSource: RemoteChartDataSource
) : MusicChartsRepository {

    override suspend fun getChart(): Result<List<Track>, DataError.Remote> {
        return remoteMusicDataSource
            .getChartSongs()
            .map { response ->
                response.tracks.tracks.map { it.toTrack() }
            }
    }

    override suspend fun searchTrack(q: String): Result<List<Track>, DataError.Remote> {
        return remoteMusicDataSource
            .searchTrack(q)
            .map { response ->
                response.tracks.map { it.toTrack() }
            }
    }

}
