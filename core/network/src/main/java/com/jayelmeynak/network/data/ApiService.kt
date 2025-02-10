package com.jayelmeynak.network.data

import com.jayelmeynak.network.data.dto.ResponseChart
import com.jayelmeynak.network.data.dto.TrackDto
import com.jayelmeynak.network.data.dto.Tracks
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @GET("/chart")
    suspend fun getChartSongs(): ResponseChart

    @GET("/search")
    suspend fun searchSongs(
        @Query("q") query: String
    ): Tracks

    @GET("/track/{id}")
    suspend fun getTrack(@Path("id") songId: String): TrackDto
}