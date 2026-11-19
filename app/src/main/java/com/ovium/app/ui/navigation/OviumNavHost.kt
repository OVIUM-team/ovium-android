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
        composable(Routes.INICIO) { HomeScreen() }
    }
}
