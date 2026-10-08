package com.jayelmeynak.search_tracks.data

import com.jayelmeynak.network.data.RemoteChartDataSource
import com.jayelmeynak.util.result.DataError
import com.jayelmeynak.util.result.Result
import com.jayelmeynak.util.result.map
import com.jayelmeynak.search_tracks.domain.models.Track
import com.jayelmeynak.search_tracks.domain.repositories.MusicChartsRepository
import javax.inject.Inject

class MusicChartsRepositoryImpl @Inject constructor(
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
