package com.example.rinasystem.ui.navigation

sealed class AriaRoutes(val route: String) {
    data object Splash : AriaRoutes("splash")
    data object Login : AriaRoutes("login")
    data object Register : AriaRoutes("register")
    data object UserDashboard : AriaRoutes("user_dashboard")
    data object AdminDashboard : AriaRoutes("admin_dashboard")
    data object Settings : AriaRoutes("settings")
    data object UpdateCheck : AriaRoutes("update_check/{role}") {
        fun withRole(role: String) = "update_check/$role"
    }
}
