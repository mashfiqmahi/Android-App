package com.example.androidbloodbank.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.androidbloodbank.data.LocalRepo
import com.example.androidbloodbank.data.model.UserProfile
import com.example.androidbloodbank.ui.*
import com.example.androidbloodbank.ui.screens.ProfileScreen
import com.example.androidbloodbank.ui.screens.RequestBloodScreen

@Composable
fun AppNavHost(navController: NavHostController, repo: LocalRepo, onAlert: (String) -> Unit = {}) {
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(snackbarHost = { SnackbarHost(hostState = snackbarHostState) }) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onLogin = { navController.navigate(Routes.LOGIN) },
                    onSignUp = { navController.navigate(Routes.SIGNUP) },
                    onEmergency = { navController.navigate(Routes.EMERGENCY) },
                    onViewDashboard = { navController.navigate(Routes.DASHBOARD) }
                )
            }
            composable("request") {
                RequestBloodScreen(
                    repo = repo,
                    onBack = { navController.popBackStack() },
                    onAlert = { msg -> onAlert(msg) }   // 👈 fixes "msg cannot infer type"
                )
            }


            composable("profile") {
                val dummyUser = UserProfile("Rahim", "O+", null, 5, "017XXXXXXXX", "Dhaka")
                ProfileScreen(
                    user = dummyUser,
                    onUpdate = { updated -> println("Updated: $updated") },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.LOGIN) {
                LoginScreen(
                    repo = repo,
                    onBack = { navController.popBackStack() },
                    onLoginSuccess = { navController.navigate(Routes.DASHBOARD) }
                )
            }
            composable(Routes.SIGNUP) {
                SignupScreen(
                    repo = repo,
                    onBack = { navController.popBackStack() },
                    onSignupSuccess = { navController.navigate(Routes.DASHBOARD) }
                )
            }
            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    repo = repo,
                    onRequest = { navController.navigate(Routes.REQUEST) },
                    onSchedules = { navController.navigate(Routes.SCHEDULES) },
                    onEmergency = { navController.navigate(Routes.EMERGENCY) }
                )
            }
            composable(Routes.REQUEST) {
                var snackbarMessage by remember { mutableStateOf<String?>(null) }
                snackbarMessage?.let { msg ->
                    LaunchedEffect(msg) {
                        snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
                        snackbarMessage = null
                    }
                }
                RequestBloodScreen(
                    repo = repo,
                    onBack = { navController.popBackStack() },
                    onAlert = { msg -> snackbarMessage = msg }
                )
            }
            composable(Routes.SCHEDULES) {
                SchedulesScreen(repo = repo, onBack = { navController.popBackStack() })
            }
            composable(Routes.EMERGENCY) {
                EmergencyScreen(repo = repo, onBack = { navController.popBackStack() })
            }
        }
    }
}
