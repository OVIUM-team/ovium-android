package com.ovium.app.ui.inventario

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ovium.app.data.local.AnimalEntity
import com.ovium.app.data.local.PartoEntity
import com.ovium.app.data.remote.ApiClient
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private val tiposParto = listOf("Simple", "Doble", "Triple", "Cuádruple", "Quíntuple")
private val codigosParto = listOf("simple", "doble", "triple", "cuadruple", "quintuple")
private val metodos = listOf("MN", "IA", "TE")

private fun PartoEntity.cerrado(ahora: LocalDateTime): Boolean =
    bloqueado || runCatching { !ahora.isBefore(LocalDateTime.parse(cierreEdicion)) }.getOrDefault(true)

private fun PartoEntity.crias(): List<CriaLocal> {
    val lista = JSONArray(criasJson ?: "[]")
    return List(lista.length()) { indice ->
        val cria = lista.getJSONObject(indice)
        CriaLocal(cria.getString("id"), cria.getString("sexo"), cria.optString("peso"))
    }
}

private data class CriaLocal(
    val id: String = UUID.randomUUID().toString(),
    val sexo: String = "hembra",
    val peso: String = ""
)

@Composable
internal fun ResumenParto(parto: PartoEntity, onEditar: () -> Unit) {
    var ahora by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(parto.cierreEdicion) {
        while (true) {
            ahora = LocalDateTime.now()
            delay(1000)
        }
    }
    val cerrado = parto.cerrado(ahora)
    Column {
        Text("Parto del ${parto.fecha}")
        Text("Ventana de edición: 30 días desde la captura")
        Text("Cierre: ${parto.cierreEdicion.replace('T', ' ').take(16)}")
        Text(if (cerrado) "Bloqueado" else "Abierto")
        val crias = remember(parto.criasJson) { parto.crias() }
        crias.forEachIndexed { indice, cria ->
            val peso = if (cria.peso.isEmpty()) "" else " · ${cria.peso} kg"
            Text("Cría ${indice + 1} · ${cria.sexo}$peso")
        }
        FotoParto(parto.fotoJpeg)
        parto.observaciones?.let { Text(it) }
        if (!cerrado) TextButton(onClick = onEditar) { Text("Editar") }
    }
}

@Composable
internal fun RegistroPartoDialog(
    madre: AnimalEntity,
    existente: PartoEntity?,
    guardar: suspend (PartoEntity) -> Unit,
    cerrar: () -> Unit
) {
    val alcance = rememberCoroutineScope()
    val resolver = LocalContext.current.contentResolver
    val id = remember { existente?.id ?: UUID.randomUUID().toString() }
    var fecha by remember { mutableStateOf(existente?.fecha ?: LocalDate.now().toString()) }
    var crias by remember { mutableStateOf(existente?.crias()?.ifEmpty { listOf(CriaLocal()) } ?: listOf(CriaLocal())) }
    var metodo by remember { mutableStateOf(existente?.metodoConcepcionId?.takeIf { it in metodos } ?: "MN") }
    var foto by remember { mutableStateOf(existente?.fotoJpeg) }
    var ocupado by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val selector = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) alcance.launch {
            ocupado = true
            try {
                foto = withContext(Dispatchers.IO) { leerFoto(resolver, uri) }
                error = if (foto == null) "No se pudo abrir la imagen." else null
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                error = "No se pudo abrir la imagen."
            } finally {
                ocupado = false
            }
        }
    }
    AlertDialog(
        onDismissRequest = { if (!ocupado) cerrar() },
        title = {
            Column {
                Text(if (existente == null) "Registrar parto" else "Editar parto")
                TextButton(enabled = !ocupado, onClick = { selector.launch("image/*") }) {
                    Text(if (foto == null) "Adjuntar foto" else "Cambiar foto")
                }
            }
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("Madre: ${madre.id}")
                Text("El registro se bloquea 30 días después de la captura.")
                OutlinedTextField(fecha, { fecha = it }, label = { Text("Fecha (AAAA-MM-DD)") }, enabled = !ocupado)
                Text("Tipo de parto: ${tiposParto[crias.size - 1]} · ${crias.size} crías")
                tiposParto.forEachIndexed { indice, tipo ->
                    TextButton(enabled = !ocupado, onClick = {
                        crias = List(indice + 1) { crias.getOrNull(it) ?: CriaLocal() }
                    }) { Text("$tipo · ${indice + 1}") }
                }
                Text("Método de concepción: $metodo")
                metodos.forEach { opcion ->
                    TextButton(enabled = !ocupado, onClick = { metodo = opcion }) { Text(opcion) }
                }
                crias.forEachIndexed { indice, cria ->
                    Text("Cría ${indice + 1} · ${cria.sexo}")
                    TextButton(enabled = !ocupado, onClick = {
                        crias = crias.toMutableList().apply {
                            set(indice, cria.copy(sexo = if (cria.sexo == "hembra") "macho" else "hembra"))
                        }
                    }) { Text("Cambiar sexo") }
                    OutlinedTextField(cria.peso, { peso ->
                        crias = crias.toMutableList().apply { set(indice, cria.copy(peso = peso)) }
                    }, label = { Text("Peso al nacer (kg, opcional)") }, enabled = !ocupado)
                }
                FotoParto(foto)
                error?.let { Text(it) }
            }
        },
        confirmButton = {
            TextButton(enabled = !ocupado, onClick = guardarParto@{
                val ahora = LocalDateTime.now()
                if (existente?.cerrado(ahora) == true) {
                    error = "La ventana de edición terminó. El parto está bloqueado."
                    return@guardarParto
                }
                val dia = runCatching { LocalDate.parse(fecha.trim()) }.getOrNull()
                if (dia == null || dia.isAfter(ahora.toLocalDate())) {
                    error = "Ingresa una fecha válida que no sea futura."
                    return@guardarParto
                }
                val imagen = foto
                if (imagen == null) {
                    error = "Adjunta una foto del parto."
                    return@guardarParto
                }
                val datos = JSONArray()
                for (cria in crias) {
                    val texto = cria.peso.trim().replace(',', '.')
                    val peso = texto.toBigDecimalOrNull()
                    if (texto.isNotEmpty() && (peso == null || peso.signum() <= 0 ||
                            peso.scale() !in 0..2 || peso > java.math.BigDecimal("999999.99"))) {
                        error = "Usa un peso positivo con hasta dos decimales."
                        return@guardarParto
                    }
                    datos.put(JSONObject().put("id", cria.id).put("sexo", cria.sexo).put("peso", texto))
                }
                val autor = existente?.registradoPor ?: usuarioParto()
                if (autor == null) {
                    error = "Inicia sesión antes de registrar un parto."
                    return@guardarParto
                }
                ocupado = true
                alcance.launch {
                    try {
                        guardar(PartoEntity(
                            id = id, madreId = madre.id, fecha = dia.toString(),
                            tipoPartoId = codigosParto[crias.size - 1], metodoConcepcionId = metodo,
                            registradoPor = autor, fechaCaptura = existente?.fechaCaptura ?: ahora.toString(),
                            cierreEdicion = existente?.cierreEdicion ?: ahora.plusDays(30).toString(),
                            padreId = existente?.padreId, observaciones = existente?.observaciones,
                            criasJson = datos.toString(), fotoJpeg = imagen, estadoSincronizacion = "pendiente"
                        ))
                        cerrar()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        error = "No se pudo guardar el parto en el dispositivo."
                    } finally {
                        ocupado = false
                    }
                }
            }) { Text(if (ocupado) "Procesando…" else "Guardar") }
        },
        dismissButton = { TextButton(enabled = !ocupado, onClick = cerrar) { Text("Cancelar") } }
    )
}

@Composable
private fun FotoParto(bytes: ByteArray?) {
    val imagen = remember(bytes) {
        bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }
    }
    if (imagen != null) Image(imagen, "Foto del parto", Modifier.height(128.dp))
}

private fun leerFoto(resolver: ContentResolver, uri: Uri): ByteArray? {
    val opciones = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opciones) }
    if (opciones.outWidth <= 0 || opciones.outHeight <= 0) return null
    var escala = 1
    while (maxOf(opciones.outWidth, opciones.outHeight) / escala > 768) escala *= 2
    opciones.inJustDecodeBounds = false
    opciones.inSampleSize = escala
    val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opciones) } ?: return null
    return try {
        ByteArrayOutputStream().use { salida ->
            if (bitmap.compress(Bitmap.CompressFormat.JPEG, 80, salida)) salida.toByteArray() else null
        }
    } finally {
        bitmap.recycle()
    }
}

private fun usuarioParto(): String? = runCatching {
    val token = requireNotNull(ApiClient.tokenGuardado())
    val datos = Base64.decode(token.split('.')[1], Base64.URL_SAFE or Base64.NO_WRAP)
    UUID.fromString(JSONObject(String(datos, Charsets.UTF_8)).getString("sub")).toString()
}.getOrNull()
