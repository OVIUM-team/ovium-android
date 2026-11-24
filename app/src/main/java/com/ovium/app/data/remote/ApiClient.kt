package com.ovium.app.data.remote

import android.content.Context
import android.content.SharedPreferences
import com.ovium.app.BuildConfig
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** Crea el cliente Retrofit. La URL viene de BuildConfig.API_URL (local.properties). */
object ApiClient {

    private lateinit var preferencias: SharedPreferences
    private val urlBase by lazy { BuildConfig.API_URL.toHttpUrl() }

    fun inicializar(contexto: Context) {
        preferencias = contexto.applicationContext.getSharedPreferences(
            "ovium_sesion", Context.MODE_PRIVATE
        )
    }

    fun tokenGuardado(): String? =
        preferencias.getString("access_token", null)?.takeIf { it.isNotBlank() }

    fun guardarToken(token: String) {
        preferencias.edit().putString("access_token", token).apply()
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor { cadena ->
            val solicitud = cadena.request()
            val url = solicitud.url
            val mismaApi = url.scheme == urlBase.scheme &&
                url.host == urlBase.host && url.port == urlBase.port
            val esLogin = url.encodedPath == urlBase.resolve("auth/login")?.encodedPath
            val token = if (mismaApi && !esLogin) tokenGuardado() else null

            val autenticada = if (token != null && solicitud.header("Authorization") == null) {
                solicitud.newBuilder().header("Authorization", "Bearer $token").build()
            } else {
                solicitud
            }
            cadena.proceed(autenticada)
        }
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
