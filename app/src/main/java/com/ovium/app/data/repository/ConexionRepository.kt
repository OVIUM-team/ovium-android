package com.ovium.app.data.repository

import com.ovium.app.data.remote.ApiClient
import com.ovium.app.data.remote.ApiService

/** Decide de dónde salen los datos. Por ahora solo prueba la conexión con la API. */
class ConexionRepository(
    private val api: ApiService = ApiClient.service
) {
    suspend fun probarConexion(): Result<Int> = try {
        val respuesta = api.health()
        if (respuesta.isSuccessful) {
            Result.success(respuesta.code())
        } else {
            Result.failure(Exception("La API respondió con el código ${respuesta.code()}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
