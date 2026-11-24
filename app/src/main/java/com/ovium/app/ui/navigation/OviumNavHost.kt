package com.ovium.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.ovium.app.data.remote.ApiClient
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ovium.app.ui.home.HomeScreen

@Composable
fun OviumNavHost(
    navController: NavHostController = rememberNavController()
) {
    val destinoInicial = remember {
        if (ApiClient.tokenGuardado() != null) Routes.INICIO else "login"
    }
    NavHost(navController = navController, startDestination = destinoInicial) {
        composable("login") {
            com.ovium.app.ui.login.LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.INICIO) {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.INICIO) { HomeScreen() }
    }
}
