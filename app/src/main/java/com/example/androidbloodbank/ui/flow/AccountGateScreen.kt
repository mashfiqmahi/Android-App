package com.example.androidbloodbank.ui.flow

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AccountGateScreen(
    onLogin: () -> Unit,
    onSignUp: () -> Unit,
    onEmergency: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Android Blood Bank",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Find donors, request blood — or use SOS offline list.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onLogin,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text("Login") }

        Spacer(Modifier.height(12.dp))

        FilledTonalButton(
            onClick = onSignUp,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text("Sign up") }

        Spacer(Modifier.height(16.dp))

        OutlinedButton(
            onClick = onEmergency,
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) { Text("Emergency SOS (offline donors)") }
    }
}
