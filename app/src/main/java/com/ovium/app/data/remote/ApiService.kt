package com.ovium.app.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import java.math.BigDecimal

/** Endpoints de la API de Ovium. Aquí se irán agregando los demás. */
interface ApiService {

    /** Llamada de prueba: solo comprueba que la API responde. */
    @GET("health")
    suspend fun health(): Response<Unit>

    @GET("ranchos")
    suspend fun listarRanchos(): List<RanchoApi>

    @GET("ranchos/{rancho}/animales")
    suspend fun listarAnimales(@Path("rancho") ranchoId: String): List<AnimalApi>

    @POST("ranchos/{rancho}/animales/{animal}/pesajes")
    suspend fun registrarPesaje(
        @Path("rancho") ranchoId: String,
        @Path("animal") animalId: String,
        @Body pesaje: RegistroPesaje
    ): Response<Unit>

    @retrofit2.http.POST("auth/login")
    suspend fun login(@retrofit2.http.Body credentials: LoginRequest): Response<LoginResponse>
}

data class LoginRequest(val username: String, val password: String)

data class LoginResponse(
    @com.google.gson.annotations.SerializedName("access_token")
    val accessToken: String?
)

data class RegistroPesaje(val id: String, val fecha: String, val tipo: String, val peso: BigDecimal)


data class RanchoApi(val id: String)

data class AnimalApi(
    val id: String,
    val sexo: String,
    val estado: String,
    @com.google.gson.annotations.SerializedName("fecha_nacimiento")
    val fechaNacimiento: String?,
    @com.google.gson.annotations.SerializedName("raza_id")
    val razaId: String?
)
