package com.example.androidbloodbank.ui.flow

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.androidbloodbank.data.LocalRepo
import com.example.androidbloodbank.data.model.RequestStatus


@Composable
fun TrackRequestScreen(repo: LocalRepo, onBack: () -> Unit) {
    val requests = remember { repo.loadRequests() } // see LocalRepo additions below
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Track Request", style = MaterialTheme.typography.headlineSmall)
        requests.forEach { r ->
            Text("• ${r.requesterName} – ${r.bloodGroup}  [${r.status}]")
        }

        TextButton(onClick = onBack) { Text("Back") }
    }
}
