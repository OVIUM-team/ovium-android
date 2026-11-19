package com.ovium.app.data.remote

import retrofit2.Response
import retrofit2.http.GET

/** Endpoints de la API de Ovium. Aquí se irán agregando los demás. */
interface ApiService {

    /** Llamada de prueba: solo comprueba que la API responde. */
    @GET("health")
    suspend fun health(): Response<Unit>
}
