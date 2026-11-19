package com.ovium.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ovium.app.data.repository.ConexionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface EstadoConexion {
    data object SinProbar : EstadoConexion
    data object Probando : EstadoConexion
    data object Conectado : EstadoConexion
    data class Error(val mensaje: String) : EstadoConexion
}

class HomeViewModel(
    private val repositorio: ConexionRepository = ConexionRepository()
) : ViewModel() {

    private val _estado = MutableStateFlow<EstadoConexion>(EstadoConexion.SinProbar)
    val estado: StateFlow<EstadoConexion> = _estado.asStateFlow()

    fun probarConexion() {
        _estado.value = EstadoConexion.Probando
        viewModelScope.launch {
            repositorio.probarConexion()
                .onSuccess { _estado.value = EstadoConexion.Conectado }
                .onFailure {
                    _estado.value = EstadoConexion.Error(it.message ?: "No se pudo conectar")
                }
        }
    }
}
