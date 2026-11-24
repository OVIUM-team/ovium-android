package com.ovium.app.ui.navigation

/** Nombres de las pantallas. Cada tarjeta nueva agrega aquí la suya. */
object Routes {
    const val INICIO = "inicio"
    const val INVENTARIO = "inventario"
    const val FICHA_ANIMAL = "animal/{animalId}"

    fun fichaAnimal(id: String): String = "animal/${android.net.Uri.encode(id)}"
}
