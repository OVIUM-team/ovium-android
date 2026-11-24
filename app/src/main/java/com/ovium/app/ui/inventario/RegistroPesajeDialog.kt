package com.ovium.app.ui.inventario

import android.util.Base64
import android.database.sqlite.SQLiteException
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
import com.ovium.app.data.local.PesajeEntity
import com.ovium.app.data.remote.ApiClient
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.temporal.ChronoUnit
import org.json.JSONObject
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
internal fun RegistroPesajeDialog(
    animal: AnimalEntity,
    historial: List<PesajeEntity>,
    guardarLocal: suspend (PesajeEntity) -> Unit,
    onCerrar: () -> Unit
) {
    val hoy = LocalDate.now(ZoneId.of("America/Mexico_City"))
    var fecha by remember { mutableStateOf(hoy.toString()) }
    var peso by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf("evaluacion") }
    var enviando by remember { mutableStateOf(false) }
    var guardado by remember { mutableStateOf(false) }
    var resumen by remember { mutableStateOf("") }
    var mensaje by remember { mutableStateOf<String?>(null) }
    val id = remember { UUID.randomUUID().toString() }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!enviando) onCerrar() },
        title = { Text("Registrar pesaje") },
        text = {
            if (guardado) {
                Text(resumen)
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
                        val usuario = usuarioDeSesion()
                        if (usuario == null) {
                            mensaje = "Inicia sesión antes de registrar un pesaje."
                            return@launch
                        }
                        val registro = PesajeEntity(
                            id = id,
                            animalId = animal.id,
                            fecha = dia.toString(),
                            tipo = tipo,
                            pesoCent = kilos.movePointRight(2).longValueExact(),
                            registradoPor = usuario,
                            estadoSincronizacion = "pendiente"
                        )
                        guardarLocal(registro)
                        resumen = "Pesaje guardado en el dispositivo. Pendiente de sincronizar.\n" +
                            resumenPesaje(animal, registro, historial)
                        guardado = true
                    } catch (_: SQLiteException) {
                        mensaje = "No se pudo guardar el pesaje en el dispositivo."
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


private fun usuarioDeSesion(): String? = runCatching {
    val token = requireNotNull(ApiClient.tokenGuardado())
    val datos = Base64.decode(token.split('.')[1], Base64.URL_SAFE or Base64.NO_WRAP)
    UUID.fromString(JSONObject(String(datos, Charsets.UTF_8)).getString("sub")).toString()
}.getOrNull()

internal fun resumenPesaje(
    animal: AnimalEntity,
    pesaje: PesajeEntity,
    historial: List<PesajeEntity>
): String {
    val fecha = LocalDate.parse(pesaje.fecha)
    val nacimiento = animal.fechaNacimiento?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    val edad = nacimiento?.takeUnless { it.isAfter(fecha) }?.let {
        var meses = ChronoUnit.MONTHS.between(it.withDayOfMonth(1), fecha.withDayOfMonth(1))
        if (it.plusMonths(meses).isAfter(fecha)) meses--
        val dias = ChronoUnit.DAYS.between(it.plusMonths(meses), fecha)
        "$meses meses y $dias días"
    } ?: "No disponible"
    val anterior = historial.filter { it.fecha < pesaje.fecha }
        .sortedWith(compareByDescending<PesajeEntity> { it.fecha }.thenBy { it.id })
        .firstOrNull()
    val ganancia = anterior?.let {
        val dias = ChronoUnit.DAYS.between(LocalDate.parse(it.fecha), fecha)
        BigDecimal.valueOf(pesaje.pesoCent - it.pesoCent, 2)
            .divide(BigDecimal.valueOf(dias), 4, RoundingMode.HALF_UP)
            .toPlainString() + " kg/día"
    } ?: "Hace falta un pesaje de una fecha anterior"
    return "Edad: $edad\nGanancia diaria: $ganancia"
}
