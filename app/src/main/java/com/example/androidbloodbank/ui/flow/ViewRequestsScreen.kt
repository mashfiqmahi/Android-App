package com.example.androidbloodbank.ui.flow

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.androidbloodbank.data.LocalRepo

@Composable
fun ViewRequestsScreen(repo: LocalRepo, onBack: () -> Unit) {
    val requests = remember { repo.loadRequests() } // see LocalRepo additions below
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Requests (${requests.size})", style = MaterialTheme.typography.headlineSmall)
        if (requests.isEmpty()) Text("No requests yet.")
        requests.forEach { r ->
            Text("• ${r.requesterName} – ${r.bloodGroup} @ ${r.hospital}")
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onBack) { Text("Back") }
    }
}
