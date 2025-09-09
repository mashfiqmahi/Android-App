package com.example.androidbloodbank.ui.flow

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DonateScreen(
    onViewRequests: () -> Unit,
    onPostRequest: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Donate blood", style = MaterialTheme.typography.headlineSmall)
        Button(onClick = onViewRequests, modifier = Modifier.fillMaxWidth()) { Text("View Requests") }
        Button(onClick = onPostRequest, modifier = Modifier.fillMaxWidth()) { Text("Post Request") }
        TextButton(onClick = onBack) { Text("Back") }
    }
}
