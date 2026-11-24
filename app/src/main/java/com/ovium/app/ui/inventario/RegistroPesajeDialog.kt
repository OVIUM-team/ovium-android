package com.ovium.app.ui.inventario

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.ovium.app.data.local.AnimalEntity
import com.ovium.app.data.remote.ApiClient
import com.ovium.app.data.remote.RegistroPesaje
import java.io.IOException
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.launch

private val tiposPesaje = linkedMapOf(
    "nacimiento" to "Nacimiento",
    "destete" to "Destete",
    "120_d" to "120 días",
    "150_d" to "150 días",
    "evaluacion" to "Evaluación"
)

@Composable
internal fun RegistroPesajeDialog(animal: AnimalEntity, onCerrar: () -> Unit) {
    val hoy = LocalDate.now(ZoneId.of("America/Mexico_City"))
    var fecha by remember { mutableStateOf(hoy.toString()) }
    var peso by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf("evaluacion") }
    var enviando by remember { mutableStateOf(false) }
    var guardado by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf<String?>(null) }
    val id = remember { UUID.randomUUID().toString() }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!enviando) onCerrar() },
        title = { Text("Registrar pesaje") },
        text = {
            if (guardado) {
                Text("Pesaje guardado en el servidor.")
            } else {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(fecha, { fecha = it }, label = { Text("Fecha (AAAA-MM-DD)") },
                        enabled = !enviando, singleLine = true)
                    OutlinedTextField(peso, { peso = it }, label = { Text("Peso (kg)") },
                        enabled = !enviando, singleLine = true)
                    Text("Tipo: ${tiposPesaje.getValue(tipo)}")
                    tiposPesaje.forEach { (codigo, etiqueta) ->
                        TextButton(onClick = { tipo = codigo }, enabled = !enviando && tipo != codigo) {
                            Text(etiqueta)
                        }
                    }
                    mensaje?.let { Text(it) }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = !enviando, onClick = {
                if (guardado) {
                    onCerrar()
                    return@TextButton
                }
                val dia = runCatching { LocalDate.parse(fecha.trim()) }.getOrNull()
                val kilos = peso.trim().replace(',', '.').toBigDecimalOrNull()
                if (dia == null || dia.isAfter(hoy)) {
                    mensaje = "Ingresa una fecha válida que no sea futura."
                    return@TextButton
                }
                if (kilos == null || kilos <= BigDecimal.ZERO ||
                    kilos > BigDecimal("999999.99") || kilos.scale() !in 0..2
                ) {
                    mensaje = "Ingresa un peso positivo con hasta dos decimales."
                    return@TextButton
                }
                enviando = true
                mensaje = null
                scope.launch {
                    try {
                        val respuesta = ApiClient.service.registrarPesaje(
                            animal.ranchoId, animal.id, RegistroPesaje(id, dia.toString(), tipo, kilos)
                        )
                        if (respuesta.isSuccessful) {
                            guardado = true
                        } else {
                            mensaje = when (respuesta.code()) {
                                401 -> "La sesión venció. Vuelve a iniciar sesión."
                                403 -> "Tu cuenta no tiene permiso para registrar pesajes."
                                404 -> "El animal no existe en el servidor."
                                409 -> "Hay un conflicto con el registro. Verifica el historial antes de reintentar."
                                422 -> "El servidor rechazó los datos del pesaje."
                                else -> "El servidor no pudo guardar el pesaje."
                            }
                        }
                    } catch (_: IOException) {
                        mensaje = "No se pudo confirmar el envío. Puedes reintentar desde esta pantalla."
                    } finally {
                        enviando = false
                    }
                }
            }) {
                Text(if (guardado) "Cerrar" else if (enviando) "Guardando…" else "Guardar")
            }
        },
        dismissButton = {
            if (!guardado) TextButton(onClick = onCerrar, enabled = !enviando) { Text("Cancelar") }
        }
    )
}
