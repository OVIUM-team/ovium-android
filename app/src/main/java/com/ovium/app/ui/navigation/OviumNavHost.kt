package com.ovium.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ovium.app.data.remote.ApiClient
import com.ovium.app.ui.home.HomeScreen
import com.ovium.app.ui.inventario.FichaAnimalScreen
import com.ovium.app.ui.inventario.InventarioScreen
import com.ovium.app.ui.login.LoginScreen

@Composable
fun OviumNavHost(
    navController: NavHostController = rememberNavController()
) {
    val destinoInicial = remember {
        if (ApiClient.tokenGuardado() != null) Routes.INICIO else "login"
    }

    NavHost(navController = navController, startDestination = destinoInicial) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.INICIO) {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.INICIO) {
            HomeScreen(onInventario = { navController.navigate(Routes.INVENTARIO) })
        }

        composable(Routes.INVENTARIO) {
            InventarioScreen(
                onAnimal = { id -> navController.navigate(Routes.fichaAnimal(id)) },
                onVolver = { navController.popBackStack() }
            )
        }

        composable(Routes.FICHA_ANIMAL) { entrada ->
            val id = requireNotNull(entrada.arguments?.getString("animalId"))
            FichaAnimalScreen(
                animalId = id,
                onVolver = { navController.popBackStack() }
            )
        }
    }
}
