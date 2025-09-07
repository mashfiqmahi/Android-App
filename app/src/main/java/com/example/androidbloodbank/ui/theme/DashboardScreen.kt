package com.example.androidbloodbank.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.androidbloodbank.data.LocalRepo
import com.example.androidbloodbank.data.model.Donor

@Composable
fun DashboardScreen(
    repo: LocalRepo,
    onRequest: () -> Unit,
    onSchedules: () -> Unit,
    onEmergency: () -> Unit,
    onProfile: () -> Unit
) {
    val donors = remember { mutableStateListOf<Donor>().apply { addAll(repo.loadDonors()) } }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Dashboard", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text("Quick Actions", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onRequest, modifier = Modifier.fillMaxWidth()) { Text("Request Blood / Find Donors") }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onSchedules, modifier = Modifier.fillMaxWidth()) { Text("View Donation Schedules") }
        Spacer(Modifier.height(8.dp))
        Button(colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB00020)), onClick = onEmergency, modifier = Modifier.fillMaxWidth()) {
            Text("Emergency Mode (SOS)", color = Color.White)
        }
        Button(onClick = onProfile, modifier = Modifier.fillMaxWidth()) {
            Text("View Profile")
        }

        Spacer(Modifier.height(16.dp))

        Text("Nearby Donors (seeded/offline):", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            donors.forEach { d ->
                Card(modifier = Modifier.fillMaxWidth().padding(6.dp)) {
                    Row(modifier = Modifier.padding(12.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(d.name, fontWeight = FontWeight.SemiBold)
                            Text("Blood: ${d.bloodGroup.label}")
                            d.phone?.let { Text("Phone: $it") }
                        }
                        if (d.verified) Text("Verified", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
