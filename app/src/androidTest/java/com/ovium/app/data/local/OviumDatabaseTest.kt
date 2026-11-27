package com.ovium.app.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OviumDatabaseTest {
    @Test
    fun guardaYLeeAnimalPartoYPesaje() = runBlocking {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        val base = Room.inMemoryDatabaseBuilder(contexto, OviumDatabase::class.java).build()

        try {
            val dao = base.localDao()
            val animal = AnimalEntity(
                id = "animal-prueba",
                ranchoId = "rancho-prueba",
                sexo = "hembra",
                estado = "activo"
            )
            val parto = PartoEntity(
                id = "parto-prueba",
                madreId = animal.id,
                fecha = "2026-11-23",
                tipoPartoId = "simple",
                metodoConcepcionId = "MN",
                registradoPor = "usuario-prueba",
                fechaCaptura = "2026-11-23T10:00:00",
                cierreEdicion = "2026-11-24T10:00:00"
            )
            val pesaje = PesajeEntity(
                id = "pesaje-prueba",
                animalId = animal.id,
                fecha = "2026-11-23",
                tipo = "evaluacion",
                pesoCent = 3250L,
                registradoPor = "usuario-prueba"
            )

            dao.guardarAnimal(animal)
            dao.guardarParto(parto)
            dao.guardarPesaje(pesaje)

            assertEquals(listOf(animal), dao.observarInventario(animal.ranchoId).first())
            assertEquals(animal, dao.observarAnimal(animal.id).first())
            assertEquals(listOf(parto), dao.observarPartos(animal.id).first())
            assertEquals(listOf(pesaje), dao.observarPesajes(animal.id).first())
            assertEquals("pendiente", dao.observarAnimal(animal.id).first()?.estadoSincronizacion)
            assertEquals("pendiente", dao.observarPartos(animal.id).first().single().estadoSincronizacion)
            assertEquals("pendiente", dao.observarPesajes(animal.id).first().single().estadoSincronizacion)
        } finally {
            base.close()
        }
    }
}
