package com.ovium.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ovium.app.ui.components.OviumButton

@Composable
fun HomeScreen(onInventario: () -> Unit, viewModel: HomeViewModel = viewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Ovium", style = MaterialTheme.typography.displayMedium)
        Text(
            text = when (val e = estado) {
                EstadoConexion.SinProbar -> "Aún no se ha probado la conexión"
                EstadoConexion.Probando -> "Conectando con la API..."
                EstadoConexion.Conectado -> "Conectado a la API"
                is EstadoConexion.Error -> "Sin conexión: ${e.mensaje}"
            },
            style = MaterialTheme.typography.titleMedium
        )
        OviumButton(
            texto = "Probar conexión con la API",
            onClick = viewModel::probarConexion,
            enabled = estado != EstadoConexion.Probando
        )
        OviumButton(texto = "Ver inventario", onClick = onInventario)
    }
}
