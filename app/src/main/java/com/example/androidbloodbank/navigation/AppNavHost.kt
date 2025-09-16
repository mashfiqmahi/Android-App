package com.example.androidbloodbank.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.androidbloodbank.data.LocalRepo

// Screens
import com.example.androidbloodbank.ui.screens.HomeScreen
import com.example.androidbloodbank.ui.screens.ProfileScreen
import com.example.androidbloodbank.ui.screens.RequestBloodScreen
import com.example.androidbloodbank.ui.LoginScreen
import com.example.androidbloodbank.ui.SignupScreen
import com.example.androidbloodbank.ui.SchedulesScreen
import com.example.androidbloodbank.ui.EmergencyScreen
import com.example.androidbloodbank.ui.flow.*

// Bottom bar

// Firebase
import com.google.firebase.auth.FirebaseAuth

@Composable
fun AppNavHost(
    navController: NavHostController,
    repo: LocalRepo,
    onAlert: (String) -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // show bottom bar on main app sections only
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val showBottomBar = when {
        route == null -> false
        route.startsWith(Route.Splash.path) -> false
        route.startsWith(Route.Gate.path)   -> false
        route.startsWith(Route.SignIn.path) -> false
        route.startsWith(Route.SignUp.path) -> false
        else -> true
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = { if (showBottomBar) BottomNavBar(navController) }
    ) { padding ->

        NavHost(
            navController = navController,
            startDestination = Route.Splash.path,
            modifier = Modifier.padding(padding)
        ) {
            // Splash — decide start based on Firebase or local session
            composable(Route.Splash.path) {
                val auth = remember { FirebaseAuth.getInstance() }
                val hasLocal = remember { repo.loadCurrentUserJson() != null }
                LaunchedEffect(Unit) {
                    val dest = if (auth.currentUser != null || hasLocal) Route.Home.path else Route.Gate.path
                    navController.navigate(dest) {
                        popUpTo(Route.Splash.path) { inclusive = true }
                        launchSingleTop = true
                    }
                }
                Box(Modifier, contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            // Gate: ONLY Login, Sign up, Emergency SOS
            composable(Route.Gate.path) {
                val auth = remember { FirebaseAuth.getInstance() }
                // If already logged in, bounce to Home
                LaunchedEffect(auth.currentUser, repo.loadCurrentUserJson()) {
                    if (auth.currentUser != null || repo.loadCurrentUserJson() != null) {
                        navController.navigate(Route.Home.path) {
                            popUpTo(Route.Gate.path) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
                AccountGateScreen(
                    onLogin = { navController.navigate(Route.SignIn.path) },
                    onSignUp = { navController.navigate(Route.SignUp.path) },
                    onEmergency = { navController.navigate(Route.EmergencySOS.path) }
                )
            }

            // Emergency SOS (offline)
            composable(Route.EmergencySOS.path) {
                EmergencySosScreen(repo = repo, onBack = { navController.popBackStack() })
            }

            // Auth
            composable(Route.SignIn.path) {
                LoginScreen(
                    repo = repo,
                    onBack = { navController.popBackStack() },
                    onLoginSuccess = {
                        navController.navigate(Route.Home.path) {
                            popUpTo(Route.Gate.path) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )

            }
            composable(Route.SignUp.path) {
                SignupScreen(
                    repo = repo,
                    onBack = { navController.popBackStack() },
                    onSignupSuccess = {
                        navController.navigate(Route.Home.path) {
                            popUpTo(Route.Gate.path) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            // HOME
            composable(Route.Home.path) {
                HomeScreen(
                    onDonate       = { navController.navigate(Route.Donate.path) },
                    onFindDonors   = { navController.navigate(Route.FindDonors.path) },
                    onBloodBank    = { navController.navigate(Route.BloodBank.path) },
                    onRequestBlood = { navController.navigate(Route.RequestBlood.path) },
                    onProfile      = { navController.navigate(Route.Profile.path) }
                )
            }

            // Donate flow
            composable(Route.Donate.path) {
                DonateScreen(
                    onViewRequests = { navController.navigate(Route.ViewRequests.path) },
                    onPostRequest  = { navController.navigate(Route.PostRequest.path) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Route.ViewRequests.path) { ViewRequestsScreen(repo = repo, onBack = { navController.popBackStack() }) }
            composable(Route.PostRequest.path)  { PostRequestScreen(repo = repo, onPosted = { navController.popBackStack() }, onBack = { navController.popBackStack() }) }

            // Find donors flow
            composable(Route.FindDonors.path) {
                FindDonorsScreen(
                    repo = repo,
                    onSelectBG = { navController.navigate(Route.SelectBloodGroup.path) },
                    onOpenDonor = { id -> navController.navigate(Route.DonorProfile.create(id)) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Route.SelectBloodGroup.path) { SelectBloodGroupScreen(onDone = { navController.popBackStack() }, onBack = { navController.popBackStack() }) }
            composable(Route.DonorProfile.path) { backStackEntry ->
                val donorId = backStackEntry.arguments?.getString(Route.DonorProfile.ArgKey) ?: return@composable
                DonorProfileScreen(donorId = donorId, repo = repo, onBack = { navController.popBackStack() })
            }

            // Blood bank flow
            composable(Route.BloodBank.path) {
                BloodBankScreen(
                    onNearby    = { navController.navigate(Route.NearbyBloodBank.path) },
                    onAvailable = { navController.navigate(Route.AvailableBlood.path) },
                    onBack      = { navController.popBackStack() }
                )
            }
            composable(Route.NearbyBloodBank.path) { NearbyBloodBankScreen(onBack = { navController.popBackStack() }) }
            composable(Route.AvailableBlood.path) { AvailableBloodScreen(onBack = { navController.popBackStack() }) }

            // Request blood
            composable(Route.RequestBlood.path) {
                var snack by remember { mutableStateOf<String?>(null) }
                snack?.let { msg ->
                    LaunchedEffect(msg) {
                        snackbarHostState.showSnackbar(message = msg, withDismissAction = true)
                        snack = null
                    }
                }
                RequestBloodScreen(
                    repo = repo,
                    onBack = { navController.popBackStack() },
                    onAlert = { msg -> snack = msg }
                )
            }
            composable(Route.TrackRequest.path) { TrackRequestScreen(repo = repo, onBack = { navController.popBackStack() }) }

            // Profile (with proper sign-out)
            composable(Route.Profile.path) {
                ProfileScreen(
                    repo = repo,
                    onBack = { navController.popBackStack() },
                    onLoggedOut = {
                        // Ensure both sessions are cleared
                        runCatching { FirebaseAuth.getInstance().signOut() }
                        repo.logoutCurrentUser()
                        navController.navigate(Route.Gate.path) {
                            popUpTo(Route.Home.path) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            // Optional legacy entries
            composable("schedules") { SchedulesScreen(repo = repo, onBack = { navController.popBackStack() }) }
            composable("emergency") { EmergencyScreen(repo = repo, onBack = { navController.popBackStack() }) }
        }
    }
}
