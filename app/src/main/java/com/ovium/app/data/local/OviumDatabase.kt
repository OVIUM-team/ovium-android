package com.ovium.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "animal",
    indices = [Index(value = ["rancho_id"])]
)
data class AnimalEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "rancho_id") val ranchoId: String,
    val sexo: String,
    val estado: String,
    @ColumnInfo(name = "madre_id") val madreId: String? = null,
    @ColumnInfo(name = "padre_id") val padreId: String? = null,
    @ColumnInfo(name = "raza_id") val razaId: String? = null,
    @ColumnInfo(name = "fecha_nacimiento") val fechaNacimiento: String? = null,
    @ColumnInfo(name = "etapa_vida") val etapaVida: String? = null,
    @ColumnInfo(name = "estado_reproductivo") val estadoReproductivo: String? = null,
    val criador: String? = null,
    val propietario: String? = null,
    @ColumnInfo(name = "circunferencia_testicular_cent") val circunferenciaTesticularCent: Long? = null,
    @ColumnInfo(name = "estado_sincronizacion", defaultValue = "'pendiente'")
    val estadoSincronizacion: String = "pendiente"
)

@Entity(
    tableName = "parto",
    foreignKeys = [
        ForeignKey(
            entity = AnimalEntity::class,
            parentColumns = ["id"],
            childColumns = ["madre_id"]
        ),
        ForeignKey(
            entity = AnimalEntity::class,
            parentColumns = ["id"],
            childColumns = ["padre_id"]
        )
    ],
    indices = [Index(value = ["madre_id", "fecha"]), Index(value = ["padre_id"])]
)
data class PartoEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "madre_id") val madreId: String,
    val fecha: String,
    @ColumnInfo(name = "tipo_parto_id") val tipoPartoId: String,
    @ColumnInfo(name = "metodo_concepcion_id") val metodoConcepcionId: String,
    @ColumnInfo(name = "registrado_por") val registradoPor: String,
    @ColumnInfo(name = "fecha_captura") val fechaCaptura: String,
    @ColumnInfo(name = "cierre_edicion") val cierreEdicion: String,
    @ColumnInfo(name = "padre_id") val padreId: String? = null,
    val bloqueado: Boolean = false,
    val observaciones: String? = null,
    @ColumnInfo(name = "crias_json") val criasJson: String? = null,
    @ColumnInfo(name = "foto_jpeg") val fotoJpeg: ByteArray? = null,
    @ColumnInfo(name = "estado_sincronizacion", defaultValue = "'pendiente'")
    val estadoSincronizacion: String = "pendiente"
)

@Entity(
    tableName = "pesaje",
    foreignKeys = [
        ForeignKey(
            entity = AnimalEntity::class,
            parentColumns = ["id"],
            childColumns = ["animal_id"]
        )
    ],
    indices = [Index(value = ["animal_id", "fecha"])]
)
data class PesajeEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "animal_id") val animalId: String,
    val fecha: String,
    val tipo: String,
    @ColumnInfo(name = "peso_cent") val pesoCent: Long,
    @ColumnInfo(name = "registrado_por") val registradoPor: String,
    @ColumnInfo(name = "estado_sincronizacion", defaultValue = "'pendiente'")
    val estadoSincronizacion: String = "pendiente"
) {
    init {
        require(pesoCent > 0) { "El peso debe ser mayor que cero." }
    }
}

@Dao
interface OviumLocalDao {
    @Query("SELECT * FROM animal ORDER BY rancho_id, id")
    fun observarAnimales(): Flow<List<AnimalEntity>>

    @Upsert
    suspend fun guardarAnimal(animal: AnimalEntity)

    @Upsert
    suspend fun guardarParto(parto: PartoEntity)

    @Upsert
    suspend fun guardarPesaje(pesaje: PesajeEntity)

    @Query("SELECT * FROM animal WHERE rancho_id = :ranchoId ORDER BY id")
    fun observarInventario(ranchoId: String): Flow<List<AnimalEntity>>

    @Query("SELECT * FROM animal WHERE id = :animalId")
    fun observarAnimal(animalId: String): Flow<AnimalEntity?>

    @Query("SELECT * FROM parto WHERE madre_id = :animalId ORDER BY fecha DESC")
    fun observarPartos(animalId: String): Flow<List<PartoEntity>>

    @Query("SELECT * FROM pesaje WHERE animal_id = :animalId ORDER BY fecha DESC")
    fun observarPesajes(animalId: String): Flow<List<PesajeEntity>>
}

@Database(
    entities = [AnimalEntity::class, PartoEntity::class, PesajeEntity::class],
    version = 2,
    exportSchema = true
)
abstract class OviumDatabase : RoomDatabase() {
    abstract fun localDao(): OviumLocalDao
}

internal val migracionPartos = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val usaColumnaAnterior = db.query("PRAGMA table_info(pesaje)").use { columnas ->
            val nombre = columnas.getColumnIndexOrThrow("name")
            generateSequence { if (columnas.moveToNext()) columnas.getString(nombre) else null }
                .any { it == "tipo_pesaje_id" }
        }
        if (usaColumnaAnterior) {
            db.execSQL("""
                CREATE TABLE pesaje_nuevo (
                    id TEXT NOT NULL PRIMARY KEY,
                    animal_id TEXT NOT NULL,
                    fecha TEXT NOT NULL,
                    tipo TEXT NOT NULL,
                    peso_cent INTEGER NOT NULL,
                    registrado_por TEXT NOT NULL,
                    estado_sincronizacion TEXT NOT NULL DEFAULT 'pendiente',
                    FOREIGN KEY(animal_id) REFERENCES animal(id) ON UPDATE NO ACTION ON DELETE NO ACTION
                )
            """.trimIndent())
            db.execSQL("""
                INSERT INTO pesaje_nuevo
                SELECT id, animal_id, fecha,
                    CASE tipo_pesaje_id WHEN 'demo-evaluacion' THEN 'evaluacion' ELSE tipo_pesaje_id END,
                    peso_cent, registrado_por, estado_sincronizacion
                FROM pesaje
            """.trimIndent())
            db.execSQL("DROP TABLE pesaje")
            db.execSQL("ALTER TABLE pesaje_nuevo RENAME TO pesaje")
            db.execSQL("CREATE INDEX index_pesaje_animal_id_fecha ON pesaje(animal_id, fecha)")
        }
        db.execSQL("ALTER TABLE parto ADD COLUMN crias_json TEXT")
        db.execSQL("ALTER TABLE parto ADD COLUMN foto_jpeg BLOB")
    }
}
