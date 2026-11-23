package com.ovium.app.data.remote

import retrofit2.Response
import retrofit2.http.GET

/** Endpoints de la API de Ovium. Aquí se irán agregando los demás. */
interface ApiService {

    /** Llamada de prueba: solo comprueba que la API responde. */
    @GET("health")
    suspend fun health(): Response<Unit>

    @retrofit2.http.POST("auth/login")
    suspend fun login(@retrofit2.http.Body credentials: LoginRequest): Response<LoginResponse>
}

data class LoginRequest(val username: String, val password: String)

data class LoginResponse(
    @com.google.gson.annotations.SerializedName("access_token")
    val accessToken: String?
)
