package com.example.androidbloodbank.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.Bloodtype
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

// Keep this file as the ONLY place that defines BottomNavBar.

private data class TabSpec(
    val label: String,
    val icon: ImageVector,
    val rootRoute: String,          // the tab’s root screen (your Route.*.path)
    val ownedPrefixes: List<String> // routes that belong to this tab (prefix match)
) {
    fun owns(route: String?): Boolean =
        route != null && ownedPrefixes.any { route.startsWith(it) }
}

@Composable
fun BottomNavBar(navController: NavHostController) {
    // Map your app's routes to tabs (adjust if your Route paths differ)
    val tabs = listOf(
        TabSpec(
            label = "Home",
            icon = Icons.Outlined.Home,
            rootRoute = Route.Home.path,
            ownedPrefixes = listOf(Route.Home.path, Route.Donate.path, Route.ViewRequests.path)
        ),
        TabSpec(
            label = "Donors",
            icon = Icons.Outlined.Search,
            rootRoute = Route.FindDonors.path,
            ownedPrefixes = listOf(Route.FindDonors.path, Route.SelectBloodGroup.path, Route.DonorProfile.path)
        ),
        TabSpec(
            label = "Request",
            icon = Icons.Outlined.AddCircle,
            rootRoute = Route.RequestBlood.path,
            ownedPrefixes = listOf(Route.RequestBlood.path, Route.PostRequest.path, Route.TrackRequest.path)
        ),
        TabSpec(
            label = "Bank",
            icon = Icons.Outlined.Bloodtype,
            rootRoute = Route.BloodBank.path,
            ownedPrefixes = listOf(Route.BloodBank.path, Route.NearbyBloodBank.path, Route.AvailableBlood.path)
        ),
        TabSpec(
            label = "Profile",
            icon = Icons.Outlined.Person,
            rootRoute = Route.Profile.path,
            ownedPrefixes = listOf(Route.Profile.path)
        )
    )

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Remember last visited route per tab root
    val lastRouteByRoot = remember { mutableStateMapOf<String, String>() }

    // Update memory whenever the route changes
    LaunchedEffect(currentRoute) {
        val tab = tabs.firstOrNull { it.owns(currentRoute) } ?: return@LaunchedEffect
        currentRoute?.let { lastRouteByRoot[tab.rootRoute] = it }
    }

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor   = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        tabs.forEach { tab ->
            val selected = tab.owns(currentRoute)

            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (selected) {
                        // Re-tap same tab -> pop this tab back to its root
                        val popped = navController.popBackStack(tab.rootRoute, inclusive = false)
                        if (!popped) {
                            navController.navigate(tab.rootRoute) { launchSingleTop = true }
                        }
                    } else {
                        // Switch tabs -> go to last route for that tab, or its root
                        val target = lastRouteByRoot[tab.rootRoute] ?: tab.rootRoute
                        navController.navigate(target) { launchSingleTop = true }
                    }
                },
                icon  = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor   = MaterialTheme.colorScheme.primary,
                    selectedTextColor   = MaterialTheme.colorScheme.primary,
                    indicatorColor      = MaterialTheme.colorScheme.surfaceVariant,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
