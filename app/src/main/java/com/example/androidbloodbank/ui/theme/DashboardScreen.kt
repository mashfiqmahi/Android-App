package com.example.androidbloodbank.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.CardDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.androidbloodbank.data.LocalRepo



@Composable
fun DashboardScreen(
    repo: LocalRepo,
    onRequest: () -> Unit,
    onSchedules: () -> Unit,
    onEmergency: () -> Unit,
    onProfile: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Dashboard", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionTile(icon = Icons.Default.Search, label = "Find Donor", onClick = onRequest)
            ActionTile(icon = Icons.Default.Person, label = "My Profile", onClick = onProfile)
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionTile(icon = Icons.Default.Schedule, label = "Schedules", onClick = onSchedules)
            ActionTile(icon = Icons.Default.Warning, label = "Emergency", onClick = onEmergency, accent = true)
        }

        Spacer(Modifier.height(12.dp))
        Text("Nearby Donors", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        // Keep your existing donor list UI here (cards) — it will scroll below
        // For brevity show a placeholder:
        Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(4.dp)) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Rahim Uddin — O- — Verified")
                Text("Ayesha Khan — A+ — Verified")
            }
        }
    }
}

@Composable
private fun ActionTile(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit, accent: Boolean = false) {
    val bg = if (accent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant
    val tint = if (accent) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ElevatedCard(
            modifier = Modifier
                .weight(1f)  // ✅ weight applied here in Row scope
                .height(120.dp)
                .clickable { onClick() },
            elevation = CardDefaults.elevatedCardElevation(6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(36.dp))
                Spacer(Modifier.height(8.dp))
                Text(label)
            }
        }
    }


}
