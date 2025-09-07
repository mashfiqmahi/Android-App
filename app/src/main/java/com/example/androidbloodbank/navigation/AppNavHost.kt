package com.example.androidbloodbank.navigation

import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.androidbloodbank.data.LocalRepo
import com.example.androidbloodbank.ui.screens.*
import com.example.androidbloodbank.data.model.UserProfile
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.example.androidbloodbank.ui.components.BottomNavBar
import com.example.androidbloodbank.ui.components.NavBarItem
import com.example.androidbloodbank.ui.LoginScreen
import com.example.androidbloodbank.ui.SignupScreen
import com.example.androidbloodbank.ui.EmergencyScreen
import com.example.androidbloodbank.ui.SchedulesScreen
@Composable
fun AppNavHost(navController: NavHostController, repo: LocalRepo, onAlert: (String) -> Unit = {}) {
    val snackbarHostState = remember { SnackbarHostState() }

    // bottom nav items
    val bottomItems = listOf(
        NavBarItem(Routes.HOME, "Home", Icons.Default.Home),
        NavBarItem(Routes.REQUEST, "Request", Icons.Default.Search),
        NavBarItem(Routes.DASHBOARD, "Dashboard", Icons.Default.Dashboard),
        NavBarItem("chat", "Chat", Icons.Default.Chat),
        NavBarItem("profile", "Profile", Icons.Default.Person)
    )

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            // Show bottomNav only on main routes (not login/signup)
            val backStackEntry by navController.currentBackStackEntryAsState()
            val current = backStackEntry?.destination?.route
            val showBottom = current in listOf(Routes.HOME, Routes.REQUEST, Routes.DASHBOARD, "chat", "profile")
            if (showBottom) BottomNavBar(navController = navController, items = bottomItems)
        }
    ) { padding ->
        NavHost(navController = navController, startDestination = Routes.HOME, modifier = Modifier.padding(padding)) {
            composable(Routes.HOME) {
                HomeScreen(
                    onLogin = { navController.navigate(Routes.LOGIN) },
                    onSignUp = { navController.navigate(Routes.SIGNUP) },
                    onEmergency = { navController.navigate(Routes.EMERGENCY) },
                   // onViewDashboard = { navController.navigate(Routes.DASHBOARD) }
                )
            }

            // request route reused for bottom nav
            composable(Routes.REQUEST) {
                var snackbarMessage by remember { mutableStateOf<String?>(null) }
                snackbarMessage?.let { msg ->
                    LaunchedEffect(msg) {
                        snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
                        snackbarMessage = null
                    }
                }
                RequestBloodScreen(repo = repo, onBack = { navController.popBackStack() }, onAlert = { m -> snackbarMessage = m })
            }

            composable("profile") {
                // example dummy user; later you can read actual profile from repo
                val dummyUser = UserProfile("Rahim", "O+", null, 5, "017XXXXXXXX", "Dhaka")
                // use the profile variant that accepts a user and callback (create it next)
                ProfileScreen(user = dummyUser, onUpdate = { /* persist via repo */ }, onBack = { navController.popBackStack() })
            }

            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    repo = repo,
                    onRequest = { navController.navigate(Routes.REQUEST) },
                    onSchedules = { navController.navigate(Routes.SCHEDULES) },
                    onEmergency = { navController.navigate(Routes.EMERGENCY) },
                    onProfile = { navController.navigate("profile") }
                )
            }

            composable(Routes.LOGIN) {
                LoginScreen(repo = repo, onBack = { navController.popBackStack() }, onLoginSuccess = { navController.navigate(Routes.DASHBOARD) })
            }
            composable(Routes.SIGNUP) {
                SignupScreen(repo = repo, onBack = { navController.popBackStack() }, onSignupSuccess = { navController.navigate(Routes.DASHBOARD) })
            }
            composable(Routes.SCHEDULES) {
                SchedulesScreen(repo = repo, onBack = { navController.popBackStack() })
            }
            composable(Routes.EMERGENCY) {
                EmergencyScreen(repo = repo, onBack = { navController.popBackStack() })
            }

            // simple placeholder for chat
            composable("chat") {
                ChatScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
