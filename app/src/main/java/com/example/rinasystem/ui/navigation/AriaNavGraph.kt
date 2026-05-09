package com.example.rinasystem.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.rinasystem.ui.screens.admin.AdminDashboardScreen
import com.example.rinasystem.ui.screens.dashboard.UserDashboardScreen
import com.example.rinasystem.ui.screens.login.LoginScreen
import com.example.rinasystem.ui.screens.register.RegisterScreen
import com.example.rinasystem.ui.screens.settings.SettingsScreen
import com.example.rinasystem.ui.screens.splash.SplashScreen
import com.example.rinasystem.ui.screens.update.UpdateCheckScreen

@Composable
fun AriaNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = AriaRoutes.Splash.route
    ) {
        composable(AriaRoutes.Splash.route) {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate(AriaRoutes.Login.route) {
                        popUpTo(AriaRoutes.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToDashboard = { role ->
                    navController.navigate(AriaRoutes.UpdateCheck.withRole(role)) {
                        popUpTo(AriaRoutes.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(AriaRoutes.Login.route) {
            LoginScreen(
                onLoginSuccess = { role ->
                    navController.navigate(AriaRoutes.UpdateCheck.withRole(role)) {
                        popUpTo(AriaRoutes.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(AriaRoutes.Register.route)
                },
                onNavigateToSettings = {
                    navController.navigate(AriaRoutes.Settings.route)
                }
            )
        }

        composable(AriaRoutes.Register.route) {
            RegisterScreen(
                onRegisterSuccess = { role ->
                    navController.navigate(AriaRoutes.UpdateCheck.withRole(role)) {
                        popUpTo(AriaRoutes.Login.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = AriaRoutes.UpdateCheck.route,
            arguments = listOf(navArgument("role") { type = NavType.StringType }),
        ) { backStackEntry ->
            val role = backStackEntry.arguments?.getString("role") ?: "user"
            UpdateCheckScreen(
                onContinue = {
                    val route = if (role == "admin") AriaRoutes.AdminDashboard.route
                    else AriaRoutes.UserDashboard.route
                    navController.navigate(route) {
                        popUpTo(AriaRoutes.UpdateCheck.route) { inclusive = true }
                    }
                }
            )
        }

        composable(AriaRoutes.UserDashboard.route) {
            UserDashboardScreen(
                onLogout = {
                    navController.navigate(AriaRoutes.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(AriaRoutes.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(AriaRoutes.AdminDashboard.route) {
            AdminDashboardScreen(
                onLogout = {
                    navController.navigate(AriaRoutes.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
