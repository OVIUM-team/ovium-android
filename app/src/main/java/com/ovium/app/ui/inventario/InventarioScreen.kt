package com.ovium.app.ui.inventario

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.room.Room
import androidx.room.withTransaction
import com.ovium.app.BuildConfig
import com.ovium.app.data.local.AnimalEntity
import com.ovium.app.data.local.OviumDatabase
import com.ovium.app.data.local.PartoEntity
import com.ovium.app.data.local.PesajeEntity
import com.ovium.app.data.remote.ApiClient
import java.io.IOException
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import java.math.BigDecimal
import kotlinx.coroutines.launch

private object BaseInventario {
    private var instancia: OviumDatabase? = null

    @Synchronized
    fun obtener(contexto: Context): OviumDatabase =
        instancia ?: Room.databaseBuilder(
            contexto.applicationContext,
            OviumDatabase::class.java,
            "ovium.db"
        ).build().also { instancia = it }
}

@Composable
fun InventarioScreen(onAnimal: (String) -> Unit, onVolver: () -> Unit) {
    val contexto = LocalContext.current.applicationContext
    val base = remember(contexto) { BaseInventario.obtener(contexto) }
    val consulta = remember(base) { base.localDao().observarAnimales() }
    val animales by consulta.collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()
    var cargandoDemo by remember { mutableStateOf(false) }
    var importando by remember { mutableStateOf(false) }
    var resultadoImportacion by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        TextButton(onClick = onVolver) { Text("Volver") }
        Text("Inventario", style = MaterialTheme.typography.headlineMedium)

        if (BuildConfig.DEBUG) {
            Button(
                enabled = !importando && !cargandoDemo,
                onClick = {
                    importando = true
                    scope.launch {
                        try {
                            val total = importarAnimalesDePrueba(base)
                            resultadoImportacion = if (total == 0) {
                                "La API no devolvió animales para esta cuenta."
                            } else {
                                "Animales disponibles en la API: $total"
                            }
                        } catch (error: HttpException) {
                            resultadoImportacion = if (error.code() == 401) {
                                "La sesión en la API venció. Inicia sesión de nuevo."
                            } else {
                                "La API devolvió HTTP ${error.code()}."
                            }
                        } catch (_: IOException) {
                            resultadoImportacion = "No se pudo conectar con la API."
                        } finally {
                            importando = false
                        }
                    }
                }
            ) {
                Text(if (importando) "Consultando API…" else "Importar animales de la API (prueba)")
            }
            resultadoImportacion?.let { Text(it) }
        }

        val registros = animales
        when {
            registros == null -> Text("Cargando inventario…")
            registros.isEmpty() -> {
                Text("Todavía no hay animales guardados en este dispositivo.")
                if (BuildConfig.DEBUG) {
                    Button(
                        enabled = !cargandoDemo && !importando,
                        onClick = {
                            cargandoDemo = true
                            scope.launch {
                                try {
                                    cargarDemo(base)
                                } finally {
                                    cargandoDemo = false
                                }
                            }
                        }
                    ) {
                        Text(if (cargandoDemo) "Cargando…" else "Cargar datos de prueba")
                    }
                }
            }
            else -> LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                registros.groupBy { it.ranchoId }.forEach { (ranchoId, grupo) ->
                    item {
                        Text("Rancho $ranchoId", style = MaterialTheme.typography.titleMedium)
                    }
                    items(grupo, key = { it.id }) { animal ->
                        Card(
                            onClick = { onAnimal(animal.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Animal ${animal.id}", style = MaterialTheme.typography.titleSmall)
                                Text("${animal.sexo} · ${animal.estado}")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FichaAnimalScreen(animalId: String, onVolver: () -> Unit) {
    val contexto = LocalContext.current.applicationContext
    val base = remember(contexto) { BaseInventario.obtener(contexto) }
    val dao = base.localDao()
    val consultaAnimal = remember(dao, animalId) { dao.observarAnimal(animalId) }
    val consultaPartos = remember(dao, animalId) { dao.observarPartos(animalId) }
    var mostrarRegistro by remember(animalId) { mutableStateOf(false) }
    val consultaPesajes = remember(dao, animalId) { dao.observarPesajes(animalId) }
    val animal by consultaAnimal.collectAsStateWithLifecycle(initialValue = null)
    val partos by consultaPartos.collectAsStateWithLifecycle(initialValue = null)
    val pesajes by consultaPesajes.collectAsStateWithLifecycle(initialValue = null)

    val animalSeleccionado = animal
    if (mostrarRegistro && animalSeleccionado != null) {
        RegistroPesajeDialog(animalSeleccionado, onCerrar = { mostrarRegistro = false })
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            TextButton(onClick = onVolver) { Text("Volver al inventario") }
            Text("Ficha del animal", style = MaterialTheme.typography.headlineMedium)
        }

        val ficha = animal
        if (ficha == null) {
            item {
                Text(
                    if (partos == null || pesajes == null) "Cargando ficha…"
                    else "El animal ya no está en el inventario local."
                )
            }
        } else {
            item {
                Text("Animal ${ficha.id}", style = MaterialTheme.typography.titleLarge)
                Text("Rancho: ${ficha.ranchoId}")
                Text("Sexo: ${ficha.sexo}")
                Text("Estado: ${ficha.estado}")
                ficha.fechaNacimiento?.let { Text("Nacimiento: $it") }
                ficha.etapaVida?.let { Text("Etapa de vida: $it") }
                ficha.estadoReproductivo?.let { Text("Estado reproductivo: $it") }
                ficha.madreId?.let { Text("Madre: $it") }
                ficha.padreId?.let { Text("Padre: $it") }
            }

            item {
                Text("Pesajes", style = MaterialTheme.typography.titleMedium)
                Button(onClick = { mostrarRegistro = true }) { Text("Registrar pesaje") }
            }
            val historialPesajes = pesajes
            when {
                historialPesajes == null -> item { Text("Cargando pesajes…") }
                historialPesajes.isEmpty() -> item { Text("Sin pesajes registrados.") }
                else -> items(historialPesajes, key = { "pesaje-${it.id}" }) { pesaje ->
                    val peso = BigDecimal.valueOf(pesaje.pesoCent, 2).toPlainString()
                    Text("${pesaje.fecha} · $peso kg")
                }
            }

            item { Text("Partos", style = MaterialTheme.typography.titleMedium) }
            val historialPartos = partos
            when {
                historialPartos == null -> item { Text("Cargando partos…") }
                historialPartos.isEmpty() -> item { Text("Sin partos registrados.") }
                else -> items(historialPartos, key = { "parto-${it.id}" }) { parto ->
                    Column {
                        Text(parto.fecha)
                        parto.observaciones?.let { Text(it) }
                    }
                }
            }
        }
    }
}

private suspend fun cargarDemo(base: OviumDatabase) = base.withTransaction {
    val dao = base.localDao()
    val animalId = "demo-001"
    dao.guardarAnimal(
        AnimalEntity(
            id = animalId,
            ranchoId = "demo",
            sexo = "hembra",
            estado = "activo",
            fechaNacimiento = "2024-02-12"
        )
    )
    dao.guardarParto(
        PartoEntity(
            id = "demo-parto-001",
            madreId = animalId,
            fecha = "2026-10-05",
            tipoPartoId = "demo-simple",
            metodoConcepcionId = "demo-mn",
            registradoPor = "demo",
            fechaCaptura = "2026-10-05T10:00:00",
            cierreEdicion = "2026-10-06T10:00:00",
            observaciones = "Parto de ejemplo"
        )
    )
    dao.guardarPesaje(
        PesajeEntity(
            id = "demo-pesaje-001",
            animalId = animalId,
            fecha = "2026-11-23",
            tipoPesajeId = "demo-evaluacion",
            pesoCent = 3250L,
            registradoPor = "demo"
        )
    )
}

private suspend fun importarAnimalesDePrueba(base: OviumDatabase): Int {
    val api = ApiClient.service
    val remotos = api.listarRanchos().flatMap { rancho ->
        api.listarAnimales(rancho.id).map { animal ->
            AnimalEntity(
                id = animal.id,
                ranchoId = rancho.id,
                sexo = animal.sexo,
                estado = animal.estado,
                fechaNacimiento = animal.fechaNacimiento,
                razaId = animal.razaId,
                estadoSincronizacion = "sincronizado"
            )
        }
    }
    base.withTransaction {
        val dao = base.localDao()
        val locales = dao.observarAnimales().first().mapTo(mutableSetOf()) { it.id }
        remotos.filter { locales.add(it.id) }.forEach { dao.guardarAnimal(it) }
    }
    return remotos.size
}
