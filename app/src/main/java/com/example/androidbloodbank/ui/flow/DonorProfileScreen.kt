package com.example.androidbloodbank.ui.flow

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.androidbloodbank.data.LocalRepo

@Composable
fun DonorProfileScreen(donorId: String, repo: LocalRepo, onBack: () -> Unit) {
    val donor = remember { repo.loadDonors().find { it.id == donorId } }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(donor?.name ?: "Unknown donor", style = MaterialTheme.typography.headlineSmall)
        Text("Blood: ${donor?.bloodGroup?.label ?: "—"}")
        Text("Verified: ${if (donor?.verified == true) "Yes" else "No"}")
        donor?.phone?.let { Text("Phone: $it") }
        TextButton(onClick = onBack) { Text("Back") }
    }
}
