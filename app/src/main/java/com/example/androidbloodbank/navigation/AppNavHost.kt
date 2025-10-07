package com.example.androidbloodbank.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.Bloodtype
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.androidbloodbank.data.LocalRepo
import com.example.androidbloodbank.ui.theme.EmergencyScreen
import com.example.androidbloodbank.ui.LoginScreen
import com.example.androidbloodbank.ui.SignupScreen
import com.example.androidbloodbank.ui.SchedulesScreen
import com.example.androidbloodbank.ui.flow.*
import com.example.androidbloodbank.ui.screens.HomeScreen
import com.example.androidbloodbank.ui.screens.ProfileScreen
import com.example.androidbloodbank.ui.screens.RequestBloodScreen
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.androidbloodbank.data.remote.FirebaseRepo
import com.example.androidbloodbank.ui.screens.ForgotPasswordScreen
import androidx.navigation.NavType
import com.example.androidbloodbank.ui.screens.EditRequestScreen


private const val TabsRoute = "tabs_shell"

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
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

        // HIDE the bottom menu in Emergency mode:
        route.startsWith(Route.EmergencySOS.path) -> false
        // (Optional: legacy/emulator routes—keep hidden if they exist)
        route.startsWith("emergency")            -> false
        else -> true
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->

        NavHost(
            navController = navController,
            startDestination = Route.Splash.path,
            modifier = Modifier.padding(padding)
        ) {
            // ---------- Splash / Gate / Auth ----------
            composable(Route.Splash.path) {
                val auth = remember { FirebaseAuth.getInstance() }
                val hasLocal = remember { repo.loadCurrentUserJson() != null }
                LaunchedEffect(Unit) {
                    val dest = if (auth.currentUser != null || hasLocal) TabsRoute else Route.Gate.path
                    navController.navigate(dest) {
                        popUpTo(Route.Splash.path) { inclusive = true }
                        launchSingleTop = true
                    }
                }
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            composable(Route.Gate.path) {
                val auth = remember { FirebaseAuth.getInstance() }
                LaunchedEffect(auth.currentUser, repo.loadCurrentUserJson()) {
                    if (auth.currentUser != null || repo.loadCurrentUserJson() != null) {
                        navController.navigate(TabsRoute) {
                            popUpTo(Route.Gate.path) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
                AccountGateScreen(
                    onLogin     = { navController.navigate(Route.SignIn.path) },
                    onSignUp    = { navController.navigate(Route.SignUp.path) },
                    onEmergency = { navController.navigate(Route.EmergencySOS.path) }
                )
            }

            composable(Route.SignIn.path) {
                LoginScreen(
                    repo = repo,
                    onBack = { navController.popBackStack() },
                    onLoginSuccess = {
                        navController.navigate(TabsRoute) {
                            popUpTo(Route.Gate.path) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onForgotPassword = {
                        navController.navigate(Route.ForgotPassword.path)
                    }
                )
            }

            composable(Route.ForgotPassword.path) {
                ForgotPasswordScreen(
                    onBack = { navController.popBackStack() },
                    onSent = { email ->
                        // Optional: show a snackbar/toast via your onAlert callback
                        onAlert("Reset link sent to $email")
                        navController.popBackStack() // return to Login
                    }
                )
            }


            composable(Route.SignUp.path) {
                SignupScreen(
                    repo = repo,
                    onBack = { navController.popBackStack() },
                    onSignupSuccess = {
                        navController.navigate(TabsRoute) {
                            popUpTo(Route.Gate.path) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(Route.EmergencySOS.path) {
                EmergencySosScreen(repo = repo, onBack = { navController.popBackStack() })
            }

            // ---------- The tabs shell (one NavController per tab) ----------
            composable(TabsRoute) {
                TabsShell(repo = repo, onLogout = {
                    runCatching { FirebaseAuth.getInstance().signOut() }
                    repo.saveCurrentUserJson(null)
                    navController.navigate(Route.Gate.path) {
                        popUpTo(TabsRoute) { inclusive = true }
                        launchSingleTop = true
                    }
                })
            }



            // Optional legacy/debug outside tabs
            composable("schedules") { SchedulesScreen(repo = repo, onBack = { navController.popBackStack() }) }
            composable("emergency") { EmergencyScreen(repo = repo, onBack = { navController.popBackStack() }) }
            composable("debugFirebase") {
                com.example.androidbloodbank.ui.debug.FirebaseDebugScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

/* ---------------- Tabs shell with 5 independent NavControllers ---------------- */

private enum class Tab { HOME, DONORS, REQUESTS, BANK, PROFILE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TabsShell(
    repo: LocalRepo,
    onLogout: () -> Unit
) {
    // One controller per tab (they persist across recompositions)
    val homeNav     = rememberNavController()
    val donorsNav   = rememberNavController()
    val requestsNav = rememberNavController()
    val bankNav     = rememberNavController()
    val profileNav  = rememberNavController()

    var currentTab by remember { mutableStateOf(Tab.HOME) }

    // Save/restore UI state when a tab leaves composition
    val stateHolder = rememberSaveableStateHolder()

    // Double-tap to root: pop this tab back stack to its start
    fun popToRoot(tab: Tab) {
        val (ctrl, startRoute) = when (tab) {
            Tab.HOME     -> homeNav to Route.Home.path
            Tab.DONORS   -> donorsNav to Route.FindDonors.path
            Tab.REQUESTS -> requestsNav to Route.RequestBlood.path
            Tab.BANK     -> bankNav to Route.BloodBank.path
            Tab.PROFILE  -> profileNav to Route.Profile.path
        }
        ctrl.popBackStack(startRoute, inclusive = false)
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentTab == Tab.HOME,
                    onClick = { if (currentTab == Tab.HOME) popToRoot(Tab.HOME) else currentTab = Tab.HOME },
                    icon = { Icon(Icons.Outlined.Home, null) }, label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = currentTab == Tab.DONORS,
                    onClick = { if (currentTab == Tab.DONORS) popToRoot(Tab.DONORS) else currentTab = Tab.DONORS },
                    icon = { Icon(Icons.Outlined.Search, null) }, label = { Text("Donors") }
                )
                NavigationBarItem(
                    selected = currentTab == Tab.REQUESTS,
                    onClick = { if (currentTab == Tab.REQUESTS) popToRoot(Tab.REQUESTS) else currentTab = Tab.REQUESTS },
                    icon = { Icon(Icons.Outlined.AddCircle, null) }, label = { Text("Request") }
                )
                NavigationBarItem(
                    selected = currentTab == Tab.BANK,
                    onClick = { if (currentTab == Tab.BANK) popToRoot(Tab.BANK) else currentTab = Tab.BANK },
                    icon = { Icon(Icons.Outlined.Bloodtype, null) }, label = { Text("Bank") }
                )
                NavigationBarItem(
                    selected = currentTab == Tab.PROFILE,
                    onClick = { if (currentTab == Tab.PROFILE) popToRoot(Tab.PROFILE) else currentTab = Tab.PROFILE },
                    icon = { Icon(Icons.Outlined.Person, null) }, label = { Text("Profile") }
                )
            }
        }
    ) { padding ->

        // Keep only the current tab composed, but SAVE the others' state
        when (currentTab) {
            /* ---------- HOME TAB ---------- */
            Tab.HOME -> stateHolder.SaveableStateProvider("tab_home") {
                NavHost(
                    navController = homeNav,
                    startDestination = Route.Home.path,
                    modifier = Modifier.padding(padding)
                ) {
                    composable(Route.Home.path) {
                        HomeScreen(
                            onDonate       = { homeNav.navigate(Route.Donate.path) },
                            onFindDonors   = { currentTab = Tab.DONORS },   // jump to tab
                            onBloodBank    = { currentTab = Tab.BANK },
                            onRequestBlood = { currentTab = Tab.REQUESTS },
                            onProfile      = { currentTab = Tab.PROFILE }
                        )
                    }
                    composable(Route.Donate.path) {
                        DonateScreen(
                            onViewRequests = { homeNav.navigate(Route.ViewRequests.path) },
                            // ✅ stay INSIDE HOME for Post Request so the form is preserved in Home stack
                            onPostRequest  = { homeNav.navigate(Route.PostRequest.path) },
                            onBack = { homeNav.popBackStack() }
                        )
                    }
                    composable(Route.ViewRequests.path) {
                        RequestsMatchedScreen(repo = repo, onBack = { homeNav.popBackStack() })
                    }
                    // ✅ Include the Post Request form INSIDE HOME tab
                    composable(Route.PostRequest.path) {
                        PostRequestScreen(
                            repo = repo,
                            onPosted = { homeNav.popBackStack() },
                            onBack   = { homeNav.popBackStack() }
                        )
                    }
                }
            }

            /* ---------- DONORS TAB ---------- */
            Tab.DONORS -> stateHolder.SaveableStateProvider("tab_donors") {
                NavHost(
                    navController = donorsNav,
                    startDestination = Route.FindDonors.path,
                    modifier = Modifier.padding(padding)
                ) {
                    composable(Route.FindDonors.path) {
                        val userDistrict: String? = null

                        FindDonorsScreen(
                            repo = repo,
                            onBack = { /* root */ },
                            onViewDetails = { donorId: String ->
                                // Navigate to donor_profile/<id>
                                donorsNav.navigate("${Route.DonorProfile.path}/$donorId")
                            }
                            // defaultDistrict = userDistrict
                        )
                    }

                    composable(Route.SelectBloodGroup.path) {
                        SelectBloodGroupScreen(
                            onDone = { donorsNav.popBackStack() },
                            onBack = { donorsNav.popBackStack() }
                        )
                    }

                    // IMPORTANT: declare a route pattern that accepts {donorId}
                    composable(
                        route = "${Route.DonorProfile.path}/{donorId}",
                        arguments = listOf(navArgument("donorId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val donorId = backStackEntry.arguments?.getString("donorId").orEmpty()
                        DonorDetailsScreen(
                            donorUid = donorId,
                            repo = repo,
                            onBack = { donorsNav.popBackStack() }
                        )
                    }
                }
            }



            /* ---------- REQUESTS TAB ---------- */
            Tab.REQUESTS -> stateHolder.SaveableStateProvider("tab_requests") {
                NavHost(requestsNav, startDestination = Route.RequestBlood.path, modifier = Modifier.padding(padding)) {
                    composable(Route.RequestBlood.path) {



                        // 🔹 Auto-cleanup this user's expired requests (private + public mirror)
                        LaunchedEffect(Unit) {
                            runCatching { FirebaseRepo().cleanupExpiredRequestsForCurrentUser() }
                        }
                        val firebaseRepo = remember { FirebaseRepo() }
                        RequestsFeedScreen(
                            repo = repo,
                            firebaseRepo = firebaseRepo,
                            onBack = { /* root */ },
                            onRequestNew = { requestsNav.navigate(Route.PostRequest.path) },
                            navController = requestsNav
                        )
                    }
                    composable(Route.PostRequest.path) {
                        PostRequestScreen(
                            repo = repo,
                            onPosted = { requestsNav.popBackStack() },
                            onBack = { requestsNav.popBackStack() }
                        )
                    }
                    composable(Route.TrackRequest.path) { TrackRequestScreen(repo = repo, onBack = { requestsNav.popBackStack() }) }

                    composable(
                        route = "${Route.EditRequest.path}/{requestId}",
                        arguments = listOf(navArgument("requestId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val requestId = backStackEntry.arguments?.getString("requestId").orEmpty()
                        EditRequestScreen(
                            requestId = requestId,
                            repo = repo,
                            onBack = { requestsNav.popBackStack() }
                        )
                    }

                }

            }




            /* ---------- BANK TAB ---------- */
            Tab.BANK -> stateHolder.SaveableStateProvider("tab_bank") {
                NavHost(bankNav, startDestination = Route.BloodBank.path, modifier = Modifier.padding(padding)) {
                    composable(Route.BloodBank.path) {
                        BloodBankScreen(
//                            onNearby    = { bankNav.navigate(Route.NearbyBloodBank.path) },
//                            onAvailable = { bankNav.navigate(Route.AvailableBlood.path) },
                              onBack      = { bankNav.popBackStack() }
                        )
                    }
                    composable(Route.NearbyBloodBank.path)  { NearbyBloodBankScreen(onBack = { bankNav.popBackStack() }) }
                    composable(Route.AvailableBlood.path)   { AvailableBloodScreen(onBack = { bankNav.popBackStack() }) }
                }
            }

            /* ---------- PROFILE TAB ---------- */
            Tab.PROFILE -> stateHolder.SaveableStateProvider("tab_profile") {
                NavHost(profileNav, startDestination = Route.Profile.path, modifier = Modifier.padding(padding)) {
                    composable(Route.Profile.path) {
                        ProfileScreen(
                            repo = repo,
                            onBack = { /* root */ },
                            onLoggedOut = onLogout
                        )
                    }
                }
            }
        }
    }
}