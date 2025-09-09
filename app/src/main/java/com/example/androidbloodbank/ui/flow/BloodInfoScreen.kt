package com.example.androidbloodbank.ui.flow

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BloodInfoScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Blood Info (education)", style = MaterialTheme.typography.headlineSmall)
        // TODO: educational content
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onBack) { Text("Back") }
    }
}
