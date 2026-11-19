package com.ovium.app.data.remote

import com.ovium.app.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** Crea el cliente Retrofit. La URL viene de BuildConfig.API_URL (local.properties). */
object ApiClient {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val service: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
