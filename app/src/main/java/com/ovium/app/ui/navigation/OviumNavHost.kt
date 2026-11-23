package com.ovium.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ovium.app.ui.home.HomeScreen

@Composable
fun OviumNavHost(
    navController: NavHostController = rememberNavController()
) {
    NavHost(navController = navController, startDestination = Routes.INICIO) {
        composable(Routes.INICIO) {
            HomeScreen(onInventario = { navController.navigate(Routes.INVENTARIO) })
        }
        composable(Routes.INVENTARIO) {
            com.ovium.app.ui.inventario.InventarioScreen(
                onAnimal = { id -> navController.navigate(Routes.fichaAnimal(id)) },
                onVolver = { navController.popBackStack() }
            )
        }
        composable(Routes.FICHA_ANIMAL) { entrada ->
            val id = requireNotNull(entrada.arguments?.getString("animalId"))
            com.ovium.app.ui.inventario.FichaAnimalScreen(
                animalId = id,
                onVolver = { navController.popBackStack() }
            )
        }
    }
}
