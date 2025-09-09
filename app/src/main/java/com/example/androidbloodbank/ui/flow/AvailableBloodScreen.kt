package com.example.androidbloodbank.ui.flow

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AvailableBloodScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Available Blood (stub)", style = MaterialTheme.typography.headlineSmall)
        // TODO: inventory UI
        TextButton(onClick = onBack) { Text("Back") }
    }
}
