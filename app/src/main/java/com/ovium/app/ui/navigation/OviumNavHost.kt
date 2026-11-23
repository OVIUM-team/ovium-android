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
    NavHost(navController = navController, startDestination = "login") {
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
