package com.example.androidbloodbank.navigation

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.Bloodtype
import androidx.compose.material.icons.outlined.Person

data class BottomItem(val route: String, val label: String, val icon: ImageVector)

@Composable
fun BottomNavBar(navController: NavHostController) {
    val items = listOf(
        BottomItem(Route.Home.path,         "Home",    Icons.Outlined.Home),
        BottomItem(Route.FindDonors.path,   "Donors",  Icons.Outlined.Search),
        BottomItem(Route.RequestBlood.path, "Request", Icons.Outlined.AddCircle),
        BottomItem(Route.BloodBank.path,    "Bank",    Icons.Outlined.Bloodtype),
        BottomItem(Route.Profile.path,      "Profile", Icons.Outlined.Person),
    )

    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor   = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        items.forEach { item ->
            val selected = currentRoute?.startsWith(item.route) == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        navController.navigate(item.route) {
                            popUpTo(Route.Home.path) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
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
