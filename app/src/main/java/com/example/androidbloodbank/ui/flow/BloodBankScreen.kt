package com.example.androidbloodbank.ui.flow

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BloodBankScreen(onNearby: () -> Unit, onAvailable: () -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Blood bank", style = MaterialTheme.typography.headlineSmall)
        Button(onClick = onNearby, modifier = Modifier.fillMaxWidth()) { Text("Nearby Blood bank") }
        Button(onClick = onAvailable, modifier = Modifier.fillMaxWidth()) { Text("See available blood") }
        TextButton(onClick = onBack) { Text("Back") }
    }
}
