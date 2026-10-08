package com.jayelmeynak.lib.network.di

import com.jayelmeynak.lib.network.BuildConfig
import com.jayelmeynak.lib.network.data.ApiService
import com.jayelmeynak.lib.network.data.RemoteChartDataSource
import com.jayelmeynak.lib.network.data.RemoteChartDataSourceImpl
import com.jayelmeynak.lib.network.data.RemoteTrackDataSource
import com.jayelmeynak.lib.network.data.RemoteTrackDataSourceImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object NetworkModule {

    private const val BASE_URL = "https://api.deezer.com"
    private const val TIMEOUT = 20L

    @Singleton
    @Provides
    fun provideOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT, TimeUnit.SECONDS)

        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
            )
        }

        return builder.build()
    }
    @Provides
    @Singleton
    fun provideApi(okHttpClient: OkHttpClient): ApiService {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class NetworkBindsModule {

    @Binds
    @Singleton
    abstract fun bindRemoteChartDataSource(impl: RemoteChartDataSourceImpl): RemoteChartDataSource

    @Binds
    @Singleton
    abstract fun bindRemoteTrackDataSource(impl: RemoteTrackDataSourceImpl): RemoteTrackDataSource
}
