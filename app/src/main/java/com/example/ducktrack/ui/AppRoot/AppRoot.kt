package com.example.ducktrack.ui.AppRoot

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

import androidx.navigation.compose.rememberNavController
import com.example.ducktrack.ui.introducePage.introduceScreen
import com.example.ducktrack.ui.login.LoginScreen

@Composable
fun AppRoot(){
    val nav = rememberNavController()

    Scaffold { inner ->
        NavHost(
            navController = nav,
            startDestination = Routes.Home,
            modifier = Modifier.padding(inner)
        ){
            composable(Routes.Home){
                introduceScreen(
                    onGoLogin = {nav.navigateSingleTop(Routes.Login)}
                )
            }
            composable(Routes.Login){
                LoginScreen (
                    onGoHome = {nav.navigateSingleTop(Routes.Home)}
                )
            }

        }
    }
}



private fun androidx.navigation.NavHostController.navigateSingleTop(route: String) {
    navigate(route) {
        launchSingleTop = true
        restoreState = true
        popUpTo(graph.startDestinationId) { saveState = true }
    }
}

